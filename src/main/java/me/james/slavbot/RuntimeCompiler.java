package me.james.slavbot;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.openhft.compiler.*;

public class RuntimeCompiler extends Thread
{
    public static final String CLASSNAME_TEMPLATE = "SlavBotCompileCommand_%d";
    public static String CODE_TEMPLATE;

    static
    {
        try
        {
            CODE_TEMPLATE = new String( Files.readAllBytes( Paths.get( "CodeTemplate.java" ) ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        Scanner scan = new Scanner( System.in );
        String line;
        while ( true )
        {
            line = scan.nextLine();

            long timestamp = System.currentTimeMillis();
            System.out.println( "Compiling... (class " + String.format( CLASSNAME_TEMPLATE, timestamp ) + ")" );
            try
            {
                Class compiled = CompilerUtils.CACHED_COMPILER.loadFromJava( String.format( CLASSNAME_TEMPLATE, timestamp ), String.format( CODE_TEMPLATE, String.format( CLASSNAME_TEMPLATE, timestamp ), line ) );
                System.out.println( "Executing..." );
                MethodInvocationUtils.invokeStaticMethod( compiled, "run", SlavBot.BOT.getBot() );
            }
            catch ( ClassNotFoundException e )
            {
                System.out.println( "Compilation failed!" );
            }
        }
    }
}
