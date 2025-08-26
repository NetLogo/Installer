#include "iconext.h"

#include <Windows.h>
#include <CommCtrl.h>
#include <commoncontrols.h>
#include <shellapi.h>
#include <ShObjIdl_core.h>

JNIEXPORT jobject JNICALL Java_org_nlogo_installer_IconExt_extractIcon(JNIEnv* env, jobject obj, jstring path)
{
    jclass clazz = env->FindClass("org/nlogo/installer/ExtResult");
    jobject empty = env->NewObject(clazz, env->GetMethodID(clazz, "<init>", "([III)V"), env->NewIntArray(0), 0, 0);

    if (!SUCCEEDED(CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED)))
    {
        return empty;
    }

    const char* chars = env->GetStringUTFChars(path, nullptr);

    size_t length = env->GetStringUTFLength(path);

    PWSTR wchars = (PWSTR)malloc(sizeof(WCHAR) * length);

    wchars[mbstowcs(wchars, chars, length)] = '\0';

    IShellItemImageFactory* imageFactory;

    if (!SUCCEEDED(SHCreateItemFromParsingName(wchars, nullptr, IID_IShellItemImageFactory , (void**)&imageFactory)))
    {
        free(wchars);

        CoUninitialize();

        return empty;
    }

    free(wchars);

    HBITMAP hbitmap;

    if (!SUCCEEDED(imageFactory->GetImage({ 256, 256 }, SIIGBF_ICONONLY, &hbitmap)))
    {
        CoUninitialize();

        return empty;
    }

    HDC dc = GetDC(nullptr);

    if (dc == nullptr)
    {
        DeleteObject(hbitmap);

        CoUninitialize();

        return empty;
    }

    BITMAP bitmap;

    if (GetObject(hbitmap, sizeof(BITMAP), &bitmap) == 0)
    {
        DeleteObject(hbitmap);

        CoUninitialize();

        return empty;
    }

    BITMAPINFOHEADER bitmapInfo;

    bitmapInfo.biSize = sizeof(BITMAPINFOHEADER);
    bitmapInfo.biWidth = bitmap.bmWidth;
    bitmapInfo.biHeight = bitmap.bmHeight;
    bitmapInfo.biPlanes = bitmap.bmPlanes;
    bitmapInfo.biBitCount = bitmap.bmBitsPixel;
    bitmapInfo.biCompression = BI_RGB;

    UINT32* bytes = (UINT32*)malloc(sizeof(UINT32) * bitmapInfo.biWidth * bitmapInfo.biHeight);

    if (GetDIBits(dc, hbitmap, 0, bitmap.bmHeight, bytes, (BITMAPINFO*)&bitmapInfo, DIB_RGB_COLORS) != bitmap.bmHeight)
    {
        DeleteObject(hbitmap);

        CoUninitialize();

        return empty;
    }

    size_t crop = 0;

    for (size_t i = 5; i < bitmap.bmHeight - 5; i++)
    {
        bool color = false;

        for (size_t j = 5; j < bitmap.bmWidth - 5; j++)
        {
            if (bytes[i * bitmap.bmWidth + j] != 0 || bytes[bitmap.bmWidth * bitmap.bmHeight - (i + 1) * bitmap.bmWidth + j] != 0 ||
                bytes[j * bitmap.bmWidth + i] != 0 || bytes[bitmap.bmWidth * bitmap.bmHeight - (j + 1) * bitmap.bmWidth + i] != 0)
            {
                color = true;

                break;
            }
        }

        if (color)
        {
            break;
        }

        crop = i + 1;
    }

    size_t croppedWidth = bitmap.bmWidth - crop * 2;
    size_t croppedHeight = bitmap.bmHeight - crop * 2;

    jintArray array = env->NewIntArray(croppedWidth * croppedHeight);

    for (size_t i = 0; i < croppedHeight; i++)
    {
        jint* croppedStart = (jint*)bytes + bitmap.bmWidth * bitmap.bmHeight - (i + crop + 1) * bitmap.bmWidth + crop;

        env->SetIntArrayRegion(array, i * croppedWidth, croppedWidth, croppedStart);
    }

    free(bytes);

    DeleteObject(hbitmap);

    CoUninitialize();

    return env->NewObject(clazz, env->GetMethodID(clazz, "<init>", "([III)V"), array, croppedWidth, croppedHeight);
}
