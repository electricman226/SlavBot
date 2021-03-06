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
    public static final String URL_REGEX = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
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
        List< String > urls = extractUrls( msg.getContent() );
        if ( urls.size() >= 1 )
            return true;
        for ( IMessage.Attachment attch : msg.getAttachments() )
        {
            String[] parts = attch.getFilename().split( Pattern.quote( "." ) );
            if ( attch.getFilename().contains( "." ) && parts.length > 0 && VALID_EXTS.contains( parts[parts.length - 1] ) )
                return true;
        }
        return false;
    }

    public static List< String > extractUrls( String text )
    {
        List< String > containedUrls = new ArrayList<>();
        Pattern pattern = Pattern.compile( URL_REGEX, Pattern.CASE_INSENSITIVE );
        Matcher urlMatcher = pattern.matcher( text );

        while ( urlMatcher.find() )
            containedUrls.add( text.substring( urlMatcher.start( 0 ), urlMatcher.end( 0 ) ) );

        return containedUrls;
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        IMessage prevMsg = lastMsgs.get( chan );
        if ( prevMsg == null )
            return "Previous message is NULL.";
        for ( IMessage.Attachment attch : prevMsg.getAttachments() )
        {
            String[] parts = attch.getFilename().split( Pattern.quote( "." ) );
            if ( attch.getFilename().contains( "." ) && parts.length > 0 && VALID_EXTS.contains( parts[parts.length - 1] ) )
                return doCommand( args, user, chan, msg, attch.getUrl() );
        }

        List< String > urls = extractUrls( prevMsg.getContent() );
        if ( urls.size() >= 1 )
            return doCommand( args, user, chan, msg, urls.get( 0 ) );

        return null;
    }

    public abstract String doCommand( String[] args, IUser user, IChannel chan, IMessage msg, String imgUrl );
}
