#include <dlfcn.h>
#include <stdio.h>
#include <unistd.h>

using InnoMain = int (*)(int, char **);

int main(int argc, char **argv) {
    if (argc != 3) {
        fprintf(stderr, "usage: %s <setup.exe> <out-dir>\n", argv[0]);
        return 64;
    }
    void *handle = dlopen("libinnoextract.so", RTLD_NOW);
    if (!handle) {
        fprintf(stderr, "dlopen failed: %s\n", dlerror());
        return 65;
    }
    auto mainFn = reinterpret_cast<InnoMain>(dlsym(handle, "main"));
    if (!mainFn) {
        fprintf(stderr, "dlsym failed: %s\n", dlerror());
        return 66;
    }
    char *args[] = {
        const_cast<char *>("innoextract"),
        const_cast<char *>("-d"),
        argv[2],
        argv[1],
        nullptr,
    };
    optind = 1;
    int rc = mainFn(4, args);
    fprintf(stderr, "innoextract main returned %d\n", rc);
    return rc;
}
