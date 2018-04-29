package me.james.slavbot.commands;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.plugins.jpeg.*;
import javax.imageio.stream.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class JPEGCommand extends SlavImageCommand
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg, String imgUrl )
    {
        float compression = 0.0f;
        if ( args.length >= 2 )
        {
            try
            {
                compression = Math.min( Math.max( 0.0f, Float.parseFloat( args[1] ) ), 1.0f );
            } catch ( NumberFormatException e ) {}
        }
        try
        {
            ImageWriter writer = ImageIO.getImageWritersByFormatName( "jpg" ).next();
            BufferedImage img = getImageFromUrl( imgUrl );
            final File f = File.createTempFile( "sb_msg_" + msg.getStringID(), "_img." + getExtension( imgUrl ) );
            f.deleteOnExit();
            writer.setOutput( new FileImageOutputStream( f ) );
            JPEGImageWriteParam params = new JPEGImageWriteParam( null );
            params.setCompressionMode( ImageWriteParam.MODE_EXPLICIT );
            params.setCompressionQuality( compression );
            writer.write( null, new IIOImage( img, null, null ), params );
            RequestBuffer.request( () -> {
                try
                {
                    chan.sendFile( f );
                } catch ( FileNotFoundException e )
                {
                    reportException( chan, e );
                    e.printStackTrace();
                }
            } );
        } catch ( IOException e )
        {
            reportException( chan, e );
            e.printStackTrace();
        }
        return null;
    }
}
