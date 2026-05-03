#ifndef EP_PLAYER_MODIFIER_H
#define EP_PLAYER_MODIFIER_H

#include <string>

namespace PlayerModifier {
	void SetConfigPath(std::string path);
	void Reload();
	void ApplyNow();
	void Tick();
	bool NoClip();
	bool InstantKill();
}

#endif
