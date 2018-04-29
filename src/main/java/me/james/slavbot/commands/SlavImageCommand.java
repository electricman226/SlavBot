package me.james.slavbot.commands;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.*;
import me.james.basebot.command.*;
import org.apache.commons.lang3.exception.*;
import sx.blah.discord.handle.obj.*;

public abstract class SlavImageCommand extends Command
{
    public static final List< String > VALID_EXTS = Arrays.asList( "png", "jpg", "jpeg", "bmp" );
    public static final int MSG_HISTORY_FARTHEST = 15;
    public static HashMap< IChannel, IMessage > lastMsgs = new HashMap<>();

    public static void reportException( IChannel chan, Exception e )
    {
        String ex = ( "Uh oh! Something went wrong...\n\n```" + ExceptionUtils.getStackTrace( e ) );
        chan.sendMessage( ex.substring( 0, Math.min( ex.length(), 1995 ) ) + "```" );
    }

    public static String getExtension( String name )
    {
        if ( !name.contains( "." ) )
            return null;
        if ( name.contains( "?" ) )
            name = name.split( Pattern.quote( "?" ) )[0];
        String[] arr = name.split( Pattern.quote( "." ) );
        return arr[arr.length - 1];
    }

    public static BufferedImage getImageFromUrl( String imgUrl ) throws IOException
    {
        HttpURLConnection url = (HttpURLConnection) new URL( imgUrl ).openConnection();
        url.addRequestProperty( "User-Agent", "Mozilla/5.0" );
        return ImageIO.read( url.getInputStream() );
    }

    public static boolean hasImage( IMessage msg )
    {
        for ( IEmbed em : msg.getEmbeds() )
            if ( em.getType().equals( "image" ) )
                return true;
        for ( IMessage.Attachment attch : msg.getAttachments() )
        {
            String[] parts = attch.getFilename().split( Pattern.quote( "." ) );
            if ( attch.getFilename().contains( "." ) && parts.length > 0 && VALID_EXTS.contains( parts[parts.length - 1] ) )
                return true;
        }
        return false;
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        IMessage prevMsg = ( !lastMsgs.containsKey( chan ) ? getLastImageMessage( chan ) : lastMsgs.get( chan ) ); // IChannel#getFullMessageHistory is expensive as FUCK. Simply cache the messages.
        if ( prevMsg == null )
            return "Previous message is NULL.";
        for ( IEmbed em : prevMsg.getEmbeds() )
            if ( em.getType().equals( "image" ) )
                return doCommand( args, user, chan, msg, em.getUrl() );

        for ( IMessage.Attachment attch : prevMsg.getAttachments() )
        {
            String[] parts = attch.getFilename().split( Pattern.quote( "." ) );
            if ( attch.getFilename().contains( "." ) && parts.length > 0 && VALID_EXTS.contains( parts[parts.length - 1] ) )
                return doCommand( args, user, chan, msg, attch.getUrl() );
        }
        return null;
    }

    public IMessage getLastImageMessage( IChannel chan )
    {
        int totalMsgs = chan.getFullMessageHistory().size();
        for ( int i = 0; i < MSG_HISTORY_FARTHEST && i <= totalMsgs; i++ )
        {
            IMessage msg = chan.getFullMessageHistory().get( i );
            if ( hasImage( msg ) )
                return msg;
        }
        return null;
    }

    public abstract String doCommand( String[] args, IUser user, IChannel chan, IMessage msg, String imgUrl );
}
