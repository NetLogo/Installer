#import <AppKit/AppKit.h>
#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>

bool setDefault(const char* path, const char* extension)
{
    NSURL* appURL = [ NSURL fileURLWithPath: [ NSString stringWithUTF8String: path ] ];
    UTType* contentType = [ UTType typeWithFilenameExtension: [ NSString stringWithUTF8String: extension ] ];

    dispatch_semaphore_t lock = dispatch_semaphore_create(0);

    __block int result = 0;

    [ [ NSWorkspace sharedWorkspace ] setDefaultApplicationAtURL: appURL toOpenContentType: contentType completionHandler: ^(NSError* error) {
        if (error)
        {
            result = 1;
        }

        dispatch_semaphore_signal(lock);
    } ];

    dispatch_semaphore_wait(lock, DISPATCH_TIME_FOREVER);

    return result == 0;
}

int main(int argc, char** argv)
{
    if (argc != 3)
    {
        return 1;
    }

    return setDefault(argv[1], "nlogo") &&
           setDefault(argv[1], "nlogox") &&
           setDefault(argv[2], "nlogo3d") &&
           setDefault(argv[2], "nlogox3d");
}
