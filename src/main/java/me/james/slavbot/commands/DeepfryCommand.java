package me.james.slavbot.commands;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class DeepfryCommand extends SlavImageCommand
{
    private BufferedImage brightenImage( BufferedImage img )
    {
        byte[] pixels = ( (DataBufferByte) img.getRaster().getDataBuffer() ).getData();

        for ( int i = 0; i < pixels.length; i++ )
        {
            Color c = new Color( (int) pixels[i] );
            float[] hsb = Color.RGBtoHSB( c.getRed(), c.getGreen(), c.getBlue(), null );
            hsb[1] -= 0.1;

            pixels[i] = (byte) Color.getHSBColor( hsb[0], hsb[1], hsb[2] ).getRGB();
        }

        return img;
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg, String imgUrl )
    {
        try
        {
            BufferedImage img = getImageFromUrl( imgUrl );
            BufferedImage lastImg = brightenImage( img );
            final File f = File.createTempFile( "sb_msg_" + msg.getStringID(), "_img." + getExtension( imgUrl ) );
            f.deleteOnExit();
            ImageIO.write( lastImg, "png", f );
            RequestBuffer.request( () -> {
                try
                {
                    chan.sendFile( f );
                }
                catch ( FileNotFoundException e )
                {
                    reportException( chan, e );
                    e.printStackTrace();
                }
            } );
        }
        catch ( IOException e )
        {
            reportException( chan, e );
            e.printStackTrace();
        }
        return null;
    }
}
