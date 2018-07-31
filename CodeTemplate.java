import sx.blah.discord.api.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import java.util.*;
import java.io.*;
import java.lang.*;

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
            System.err.println(String.format("THREW INTERNAL EXCEPTION: %s (%s)", internal_exception.getClass().getSimpleName(), internal_exception.getMessage()));
        }
    }
}