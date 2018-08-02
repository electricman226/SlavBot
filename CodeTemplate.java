import sx.blah.discord.api.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import java.util.*;
import java.io.*;
import java.lang.*;
import static me.james.slavbot.RuntimeCompilerHelpers.*;

public class %s
{   
    public static void run( IDiscordClient client )
    {
        try
        {
            %s
        }
        catch(Exception internal_exception)
        {
            System.err.println("THREW INTERNAL EXCEPTION: " + internal_exception.getClass().getSimpleName() + " (" + internal_exception.getMessage() + ")");
        }
    }
}