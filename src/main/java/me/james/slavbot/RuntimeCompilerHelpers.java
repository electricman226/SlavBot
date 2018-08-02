package me.james.slavbot;

import java.io.*;
import java.nio.file.*;

// This is meant to be statically imported
public class RuntimeCompilerHelpers
{

    public static void writeln( String text )
    {
        writeln( text, new File( "runtime_compiler_temp_" + System.currentTimeMillis() / 1000 ) );
    }

    public static void writeln( String text, File f )
    {
        try
        {
            if ( !f.exists() )
                f.createNewFile();
            Files.write( f.toPath(), ( text + "\n" ).getBytes(), StandardOpenOption.APPEND );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }
}
