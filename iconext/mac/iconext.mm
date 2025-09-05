#include "iconext.h"

#include <AppKit/AppKit.h>

JNIEXPORT jobject JNICALL Java_org_nlogo_installer_IconExt_extractIcon(JNIEnv* env, jobject obj, jstring path)
{
    jclass clazz = env->FindClass("org/nlogo/installer/ExtResult");
    jobject empty = env->NewObject(clazz, env->GetMethodID(clazz, "<init>", "([III)V"), env->NewIntArray(0), 0, 0);

    jboolean copy;

    const char* chars = env->GetStringUTFChars(path, &copy);

    NSImage* icns = [[NSImage alloc] initWithContentsOfURL: [NSURL fileURLWithPath: [NSString stringWithUTF8String: chars]]];

    env->ReleaseStringUTFChars(path, chars);

    if ([icns representations] == nil || [[icns representations] count] == 0)
    {
        return empty;
    }

    NSImageRep* largest = [icns representations][0];

    for (NSImageRep* representation : [icns representations])
    {
        if ([representation size].width > [largest size].width)
        {
            largest = representation;
        }
    }

    CGImageRef image = [largest CGImageForProposedRect: nil context: nil hints: nil];

    size_t width = [largest pixelsWide];
    size_t height = [largest pixelsHigh];

    uint32_t* pixels = (uint32_t*)calloc(sizeof(uint32_t), width * height);

    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();

    CGContextRef context = CGBitmapContextCreate(pixels, width, height, 8, width * sizeof(uint32_t), space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrderDefault);

    CGColorSpaceRelease(space);

    CGContextDrawImage(context, CGRectMake(0, 0, width, height), image);
    CGContextRelease(context);

    for (size_t i = 0; i < width * height; i++)
    {
        uint8_t a = pixels[i] >> 24;
        uint8_t r = (pixels[i] >> 16) & 0xFF;
        uint8_t g = (pixels[i] >> 8) & 0xFF;
        uint8_t b = pixels[i] & 0xFF;

        pixels[i] = (a << 24) | (b << 16) | (g << 8) | r;
    }

    jintArray array = env->NewIntArray(width * height);

    env->SetIntArrayRegion(array, 0, width * height, (jint*)pixels);

    free(pixels);

    return env->NewObject(clazz, env->GetMethodID(clazz, "<init>", "([III)V"), array, width, height);
}
