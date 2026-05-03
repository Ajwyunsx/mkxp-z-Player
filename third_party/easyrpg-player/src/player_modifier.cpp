#include "player_modifier.h"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <map>
#include <string>
#include <utility>

#include <lcf/data.h>

#include "game_actor.h"
#include "game_party.h"
#include "main_data.h"

namespace {
	struct ModifierState {
		bool infinite_hp = false;
		bool infinite_mp = false;
		bool instant_kill = false;
		bool no_clip = false;
		bool all_items = false;
		bool gold_enabled = false;
		int gold = 999999;
		bool exp_enabled = false;
		int exp = 999999;
		long long apply_nonce = 0;
	};

	std::string config_path;
	ModifierState state;
	long long last_apply_nonce = -1;
	int tick_counter = 0;

	std::string Trim(std::string value) {
		auto not_space = [](unsigned char ch) { return !std::isspace(ch); };
		value.erase(value.begin(), std::find_if(value.begin(), value.end(), not_space));
		value.erase(std::find_if(value.rbegin(), value.rend(), not_space).base(), value.end());
		return value;
	}

	bool BoolValue(const std::map<std::string, std::string>& props, const char* key, bool fallback) {
		auto it = props.find(key);
		if (it == props.end()) {
			return fallback;
		}
		std::string value = it->second;
		std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
			return static_cast<char>(std::tolower(ch));
		});
		if (value == "1" || value == "true" || value == "yes") {
			return true;
		}
		if (value == "0" || value == "false" || value == "no") {
			return false;
		}
		return fallback;
	}

	int IntValue(const std::map<std::string, std::string>& props, const char* key, int fallback) {
		auto it = props.find(key);
		if (it == props.end()) {
			return fallback;
		}
		try {
			return std::max(0, std::min(99999999, std::stoi(it->second)));
		} catch (...) {
			return fallback;
		}
	}

	long long LongValue(const std::map<std::string, std::string>& props, const char* key, long long fallback) {
		auto it = props.find(key);
		if (it == props.end()) {
			return fallback;
		}
		try {
			return std::max(0LL, std::stoll(it->second));
		} catch (...) {
			return fallback;
		}
	}

	void LoadState() {
		if (config_path.empty()) {
			return;
		}
		std::ifstream input(config_path);
		if (!input) {
			return;
		}
		std::map<std::string, std::string> props;
		std::string line;
		while (std::getline(input, line)) {
			auto pos = line.find('=');
			if (pos == std::string::npos) {
				continue;
			}
			props[Trim(line.substr(0, pos))] = Trim(line.substr(pos + 1));
		}
		state.infinite_hp = BoolValue(props, "infiniteHp", state.infinite_hp);
		state.infinite_mp = BoolValue(props, "infiniteMp", state.infinite_mp);
		state.instant_kill = BoolValue(props, "instantKill", state.instant_kill);
		state.no_clip = BoolValue(props, "noClip", state.no_clip);
		state.all_items = BoolValue(props, "allItems", state.all_items);
		state.gold_enabled = BoolValue(props, "goldEnabled", state.gold_enabled);
		state.gold = IntValue(props, "gold", state.gold);
		state.exp_enabled = BoolValue(props, "expEnabled", state.exp_enabled);
		state.exp = IntValue(props, "exp", state.exp);
		state.apply_nonce = LongValue(props, "applyNonce", state.apply_nonce);
	}

	void ApplyActor(Game_Actor* actor) {
		if (!actor) {
			return;
		}
		if (state.infinite_hp) {
			actor->SetHp(actor->GetMaxHp());
		}
		if (state.infinite_mp) {
			actor->SetSp(actor->GetMaxSp());
		}
		if (state.exp_enabled && state.apply_nonce != last_apply_nonce) {
			actor->ChangeExp(state.exp, nullptr);
		}
	}

	void ApplyPartyOnce() {
		if (!Main_Data::game_party) {
			return;
		}
		if (state.gold_enabled) {
			int current = Main_Data::game_party->GetGold();
			int delta = state.gold - current;
			if (delta > 0) {
				Main_Data::game_party->GainGold(delta);
			} else if (delta < 0) {
				Main_Data::game_party->LoseGold(-delta);
			}
		}
		if (state.all_items && state.apply_nonce != last_apply_nonce) {
			for (const auto& item: lcf::Data::items) {
				if (item.ID > 0) {
					int missing = Main_Data::game_party->GetMaxItemCount(item.ID) - Main_Data::game_party->GetItemCount(item.ID);
					if (missing > 0) {
						Main_Data::game_party->AddItem(item.ID, missing);
					}
				}
			}
		}
		for (auto* actor: Main_Data::game_party->GetActors()) {
			ApplyActor(actor);
		}
		last_apply_nonce = state.apply_nonce;
	}
}

namespace PlayerModifier {
	void SetConfigPath(std::string path) {
		config_path = std::move(path);
		Reload();
	}

	void Reload() {
		LoadState();
	}

	void ApplyNow() {
		LoadState();
		ApplyPartyOnce();
	}

	void Tick() {
		if (++tick_counter >= 30) {
			tick_counter = 0;
			LoadState();
		}
		ApplyPartyOnce();
	}

	bool NoClip() {
		return state.no_clip;
	}

	bool InstantKill() {
		return state.instant_kill;
	}
}
