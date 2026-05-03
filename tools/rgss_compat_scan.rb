#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "optparse"
require "zlib"

class Rgss3Archive
  MAGIC = 0xDEADCAFE

  Entry = Struct.new(:name, :offset, :size, :magic, keyword_init: true)

  def initialize(data)
    @data = data.b
    parse
  end

  def entries
    @entries.values
  end

  def read(path)
    entry = @entries[path.tr("\\", "/").downcase]
    return nil unless entry

    encrypted = @data.byteslice(entry.offset, entry.size).to_s.b
    decrypt_entry(encrypted, entry.magic)
  end

  private

  def parse
    raise "not an RGSS3A archive" unless @data.byteslice(0, 7) == "RGSSAD\0" && @data.getbyte(7) == 3

    pos = 8
    base = (read_u32(pos) * 9 + 3) & 0xffffffff
    pos += 4
    @entries = {}

    loop do
      offset = read_u32(pos) ^ base
      pos += 4
      break if offset.zero?

      size = read_u32(pos) ^ base
      pos += 4
      magic = read_u32(pos) ^ base
      pos += 4
      name_len = read_u32(pos) ^ base
      pos += 4

      raw_name = @data.byteslice(pos, name_len).to_s.b
      pos += name_len
      name = raw_name.bytes.each_with_index.map { |byte, index| byte ^ ((base >> (8 * (index % 4))) & 0xff) }
      name = name.pack("C*").force_encoding("UTF-8").scrub.tr("\\", "/")
      @entries[name.downcase] = Entry.new(name: name, offset: offset, size: size, magic: magic)
    end
  end

  def read_u32(offset)
    @data.byteslice(offset, 4).unpack1("V")
  end

  def decrypt_entry(encrypted, magic)
    output = String.new(capacity: encrypted.bytesize, encoding: Encoding::BINARY)
    offset = 0
    while offset < encrypted.bytesize
      chunk_size = [4, encrypted.bytesize - offset].min
      value = encrypted.byteslice(offset, chunk_size).ljust(4, "\0").unpack1("V")
      value ^= magic
      output << [value].pack("V").byteslice(0, chunk_size)
      magic = (magic * 7 + 3) & 0xffffffff if chunk_size == 4
      offset += chunk_size
    end
    output
  end
end

ScriptEntry = Struct.new(:index, :name, :code, keyword_init: true)

class RgssCompatibilityScanner
  RGSS_MARKER = "RGSSAD\0".b
  SCRIPT_NAMES = [
    "Data/Scripts.rvdata2",
    "Data/Scripts.rvdata",
    "Data/Scripts.rxdata"
  ].freeze

  API_RE = /(Win32API|WinAPI|MiniFFI)\.new\s*\(?\s*([^,\n]+)\s*,\s*["']([^"']+)["']/i
  CONST_RE = /^\s*([A-Z][A-Za-z0-9_]*)\s*=\s*["']([^"']+)["']/

  def initialize(root)
    @root = File.expand_path(root)
  end

  def scan
    scripts = load_scripts
    {
      root: @root,
      script_count: scripts.length,
      syntax_errors: syntax_errors(scripts),
      win32_api_calls: win32_api_calls(scripts),
      android_api_mappings: android_api_mappings(win32_api_calls(scripts)),
      compatibility_flags: compatibility_flags(scripts)
    }
  end

  private

  def load_scripts
    data = find_scripts_data
    list = Marshal.load(data)
    list.each_with_index.map do |entry, index|
      name = entry[1].to_s
      code = Zlib.inflate(entry[2].to_s)
      ScriptEntry.new(index: index, name: name, code: code.force_encoding("UTF-8").scrub)
    end
  end

  def find_scripts_data
    return File.binread(@root) if File.file?(@root) && @root =~ /Scripts\.r(?:x|v)data2?\z/i

    if File.directory?(@root)
      SCRIPT_NAMES.each do |name|
        path = File.join(@root, name)
        return File.binread(path) if File.file?(path)
      end

      archive = Dir.children(@root).find { |name| name =~ /\.rgss3a\z/i }
      return read_scripts_from_rgss3(File.binread(File.join(@root, archive))) if archive

      executable = Dir.children(@root).find { |name| name =~ /\.exe\z/i }
      return read_scripts_from_rgss3(extract_embedded_rgss(File.join(@root, executable))) if executable
    end

    raise "Scripts.rvdata2 / Game.rgss3a / packed exe not found: #{@root}"
  end

  def extract_embedded_rgss(executable)
    data = File.binread(executable)
    offset = data.index(RGSS_MARKER)
    raise "embedded RGSS archive not found: #{executable}" unless offset

    data.byteslice(offset, data.bytesize - offset)
  end

  def read_scripts_from_rgss3(data)
    archive = Rgss3Archive.new(data)
    SCRIPT_NAMES.each do |name|
      scripts = archive.read(name)
      return scripts if scripts
    end
    available = archive.entries.map(&:name).grep(/Scripts\.r/i)
    raise "scripts file not found in RGSS3A archive; candidates=#{available.join(", ")}"
  end

  def syntax_errors(scripts)
    scripts.filter_map do |script|
      begin
        RubyVM::InstructionSequence.compile(script.code, script.name)
        nil
      rescue SyntaxError => error
        {
          index: script.index,
          script: script.name,
          message: error.message.lines.first.to_s.strip
        }
      end
    end
  end

  def win32_api_calls(scripts)
    scripts.flat_map do |script|
      constants = script.code.lines.each_with_object({}) do |line, map|
        match = line.match(CONST_RE)
        map[match[1]] = match[2] if match
      end

      script.code.lines.each_with_index.filter_map do |line, line_index|
        match = line.match(API_RE)
        next unless match

        library_expr = match[2].strip
        library = literal_or_constant(library_expr, constants)
        {
          index: script.index,
          script: script.name,
          line: line_index + 1,
          api: match[1],
          library: library,
          function: match[3],
          source: line.strip
        }
      end
    end
  end

  def literal_or_constant(value, constants)
    if (match = value.match(/\A["']([^"']+)["']\z/))
      match[1]
    else
      constants[value] || value
    end
  end

  def compatibility_flags(scripts)
    all_code = scripts.map(&:code).join("\n")
    flags = []
    flags << "win32api" if all_code.match?(/Win32API|WinAPI|MiniFFI|DL\.dlopen|Fiddle\.dlopen/i)
    flags << "windows_core_dll" if all_code.match?(/kernel32|user32|gdi32|shell32|advapi32/i)
    flags << "native_bitmap_dll" if all_code.match?(/tktk_bitmap|PngSaveA|RtlMoveMemory/i)
    flags << "ruby_classic" if all_code.match?(/\bid\b|\btype\b|Hash#index|Thread\.critical|\$KCODE/)
    flags.uniq
  end

  def android_api_mappings(calls)
    calls.map do |call|
      {
        script: call[:script],
        line: call[:line],
        source: "#{call[:library]}!#{call[:function]}",
        android_mapping: android_mapping_for(call[:library], call[:function])
      }
    end
  end

  def android_mapping_for(library, function)
    lib = library.to_s.downcase.sub(/\.dll\z/, "")
    fn = function.to_s.downcase
    case [lib, fn]
    in ["tktk_bitmap", "pngsavea" | "pngsavew" | "pngsave"]
      "player save-thumbnail placeholder via Android writable save path"
    in ["tktk_bitmap", "getgamehwnd"]
      "player pseudo window handle"
    in ["tktk_bitmap", "getaddress" | "getpixeldata" | "setpixeldata" | "blur" | "changetone" | "clipmask" | "invertcolor" | "mosaic" | "blendblt" | "changesize"]
      "safe no-op / success fallback in player compatibility layer"
    in [_, "findwindow" | "findwindowa" | "findwindoww" | "getforegroundwindow"]
      "player pseudo window handle"
    in [_, "getdesktopwindow"]
      "android display handle"
    in [_, "getsystemmetrics"]
      "Android.display_metrics"
    in [_, "systemparametersinfo" | "systemparametersinfoa" | "systemparametersinfow"]
      "Android display work-area rect"
    in [_, "getclientrect" | "getwindowrect"]
      "Graphics size / Android display rect"
    in [_, "setwindowpos" | "movewindow"]
      "Graphics.resize_screen when size changes"
    in ["kernel32", "getprivateprofilestring" | "getprivateprofilestringa" | "getprivateprofilestringw"]
      "Ruby Game.ini parser"
    in ["kernel32", "copyfile" | "copyfilea" | "copyfilew"]
      "Ruby File copy"
    in ["kernel32", "rtlmovememory" | "copymemory"]
      "safe Ruby buffer copy fallback"
    in [_, "keybd_event"]
      "safe no-op; Android key injection is not allowed for apps"
    else
      lib.start_with?("user32") || lib.start_with?("kernel32") ? "safe Android fallback" : "native MiniFFI if library exists"
    end
  end
end

options = { json: false }
parser = OptionParser.new do |opts|
  opts.banner = "Usage: ruby tools/rgss_compat_scan.rb [--json] GAME_DIR_OR_SCRIPTS"
  opts.on("--json", "print JSON report") { options[:json] = true }
end
parser.parse!

target = ARGV.shift
abort parser.to_s unless target

report = RgssCompatibilityScanner.new(target).scan

if options[:json]
  puts JSON.pretty_generate(report)
else
  puts "RGSS compatibility scan"
  puts "root: #{report[:root]}"
  puts "scripts: #{report[:script_count]}"
  puts "flags: #{report[:compatibility_flags].join(", ")}"
  puts
  puts "syntax errors: #{report[:syntax_errors].length}"
  report[:syntax_errors].each do |error|
    puts "  [#{error[:index]}] #{error[:script]}: #{error[:message]}"
  end
  puts
  puts "Win32API calls: #{report[:win32_api_calls].length}"
  report[:win32_api_calls].each do |call|
    puts "  [#{call[:index]}] #{call[:script]}:#{call[:line]} #{call[:library]}!#{call[:function]}"
  end
  puts
  puts "Android mappings:"
  report[:android_api_mappings].each do |mapping|
    puts "  #{mapping[:source]} -> #{mapping[:android_mapping]}"
  end
end
