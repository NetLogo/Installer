#include <string>

#include <shlobj.h>
#include <winreg.h>

#define CHECK(expr) if (expr) return expr;

int setCommand(const std::string& ext, const std::string& desc, const std::string& cmd)
{
    const std::string subKey = "NetLogo." + ext + "\\shell\\open";

    HKEY key;

    CHECK(RegCreateKeyEx(HKEY_CLASSES_ROOT, subKey.c_str(), 0, nullptr, 0, KEY_READ | KEY_WRITE, nullptr, &key, nullptr));
    CHECK(RegSetValue(key, nullptr, REG_SZ, desc.c_str(), 0));
    CHECK(RegSetValue(key, "command", REG_SZ, cmd.c_str(), 0));
    CHECK(RegDeleteKeyValue(key, "command", "command"));
    CHECK(RegCloseKey(key));

    return 0;
}

int main(int argc, char** argv)
{
    if (argc != 3)
    {
        return argc;
    }

    const std::string twodPath = std::string(argv[1]) + "\\NetLogo.exe";
    const std::string threedPath = std::string(argv[1]) + "\\NetLogo 3D.exe";

    const std::string twodCmd = "\"" + twodPath + "\" --launch \"%1\"";
    const std::string threedCmd = "\"" + threedPath + "\" --launch \"%1\"";

    const std::string twodDesc = "Edit with NetLogo " + std::string(argv[2]);
    const std::string threedDesc = "Edit with NetLogo 3D " + std::string(argv[2]);

    CHECK(setCommand("nlogo", twodDesc, twodCmd));
    CHECK(setCommand("nlogox", twodDesc, twodCmd));
    CHECK(setCommand("nlogo3d", threedDesc, threedCmd));
    CHECK(setCommand("nlogox3d", threedDesc, threedCmd));

    SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, NULL, NULL);

    return 0;
}
