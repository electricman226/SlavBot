package me.james.slavbot;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.io.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import sx.blah.discord.handle.obj.*;

public class GuildLog
{
    public static final File LOG_DIRECTORY = new File( "guildlogs" );
    public static final File FILE_CACHE_DIRECTORY = new File( LOG_DIRECTORY.getPath() + "//files" );
    public static HashMap< IGuild, GuildLog > LOGS = new HashMap<>();

    static
    {
        if ( !LOG_DIRECTORY.exists() )
            LOG_DIRECTORY.mkdir();
        if ( !FILE_CACHE_DIRECTORY.exists() )
            FILE_CACHE_DIRECTORY.mkdir();
    }

    public File f;
    public JsonObject log;
    public IGuild guild;

    private GuildLog( IGuild guild )
    {
        this.guild = guild;
        f = new File( LOG_DIRECTORY.getPath() + "//" + guild.getStringID() + ".json" );
        if ( !f.exists() )
            init();
        log = SlavBot.fileToJSON( f );
    }

    public static List< String > extractUrls( String text )
    {
        List< String > containedUrls = new ArrayList<>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile( urlRegex, Pattern.CASE_INSENSITIVE );
        Matcher urlMatcher = pattern.matcher( text );

        while ( urlMatcher.find() )
            containedUrls.add( text.substring( urlMatcher.start( 0 ), urlMatcher.end( 0 ) ) );

        return containedUrls;
    }

    public static GuildLog create( IGuild g )
    {
        SlavBot.BOT.getLogger().info( "Creating log for guild " + g.getName() + " (" + g.getStringID() + ")" );
        GuildLog log = new GuildLog( g );
        return LOGS.put( g, log );
    }

    public static GuildLog getLogForGuild( IGuild g )
    {
        return LOGS.get( g );
    }

    private void init()
    {
        init( true );
    }

    private void init( boolean repop )
    {
        log = new JsonObject();
        JsonObject guild_info = new JsonObject();
        guild_info.addProperty( "name", guild.getName() );
        // TODO: Add more guild specific info.
        // FIXME: What if the name changes? What about other guild information?
        log.add( "guild_info", guild_info );
        log.add( "channels", new JsonArray() );
        log.add( "messages", new JsonArray() );
        for ( IChannel chan : guild.getChannels() )
            logChannel( chan );
        if ( repop )
            forceRepopulate();
        save();
    }

    public void logChannel( IChannel chan )
    {
        logChannel( chan, true );
    }

    public void logChannel( IChannel chan, boolean save )
    {
        JsonArray channels = log.get( "channels" ).getAsJsonArray();
        JsonObject chanObj = new JsonObject();

        chanObj.addProperty( "id", chan.getStringID() );
        chanObj.addProperty( "name", chan.getName() );
        chanObj.addProperty( "permission_bits", Permissions.generatePermissionsNumber( chan.getModifiedPermissions( SlavBot.BOT.getBot().getOurUser() ) ) );
        chanObj.addProperty( "topic", chan.getTopic() );
        if ( chan.getCategory() != null )
            chanObj.addProperty( "parent", chan.getCategory().getName() );

        channels.add( chanObj );
        if ( save )
            save();
    }

    public void logMessage( IMessage msg )
    {
        logMessage( msg, true );
    }

    public void logMessage( IMessage msg, boolean save )
    {
        JsonArray messages = log.get( "messages" ).getAsJsonArray();

        JsonObject msgObj = new JsonObject();
        msgObj.addProperty( "id", msg.getStringID() );
        msgObj.addProperty( "channel_id", msg.getChannel().getStringID() );
        msgObj.addProperty( "channel", msg.getChannel().getName() );
        msgObj.addProperty( "channel_topic", msg.getChannel().getTopic() );
        msgObj.addProperty( "author_id", msg.getAuthor().getStringID() );
        msgObj.addProperty( "author", msg.getAuthor().getName() );
        msgObj.addProperty( "author_nick", msg.getAuthor().getNicknameForGuild( msg.getGuild() ) );
        msgObj.addProperty( "timestamp", msg.getTimestamp().getEpochSecond() );
        msgObj.addProperty( "content", msg.getContent() );
        JsonArray roles = new JsonArray();
        for ( IRole r : msg.getAuthor().getRolesForGuild( msg.getGuild() ) )
        {
            JsonObject roleObj = new JsonObject();
            roleObj.addProperty( "id", r.getStringID() );
            roleObj.addProperty( "name", r.getName() );
            roleObj.addProperty( "color", r.getColor().getRGB() );
            roleObj.addProperty( "permission_bits", Permissions.generatePermissionsNumber( r.getPermissions() ) );
            roles.add( roleObj );
        }
        msgObj.add( "current_roles", roles );
        msgObj.addProperty( "overall_permissions_bits", Permissions.generatePermissionsNumber( msg.getAuthor().getPermissionsForGuild( msg.getGuild() ) ) );

        if ( msg.getAttachments().size() > 0 )
        {
            JsonArray attachments = new JsonArray();

            for ( IMessage.Attachment a : msg.getAttachments() )
            {
                JsonObject aObj = new JsonObject();
                try
                {
                    // We have to do it like this instead of using IOUtils#toByteArray(URL) because Discord gay lmao.
                    logFile( new URL( a.getUrl() ), msg.getStringID() + "_" + a.getFilename() );
                    aObj.addProperty( "id", a.getStringID() );
                    aObj.addProperty( "url", a.getUrl() );
                    aObj.addProperty( "name", a.getFilename() );

                    aObj.addProperty( "_archive_time", System.currentTimeMillis() / 1000 );
                    attachments.add( aObj );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
            msgObj.add( "attachments", attachments );
        }

        messages.add( msgObj );
        if ( save )
            save();
    }

    public void logFile( URL url, String p ) throws IOException
    {
        HttpGet req = new HttpGet( url.toString() );
        HttpClient client = HttpClients.custom().setUserAgent( "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1" ).build();
        HttpResponse resp = client.execute( req );
        Files.write( Paths.get( FILE_CACHE_DIRECTORY.getPath() + "//" + p ), IOUtils.toByteArray( resp.getEntity().getContent() ), StandardOpenOption.CREATE );
    }

    private void save()
    {
        try
        {
            if ( !f.exists() )
                f.createNewFile();
            Files.write( f.toPath(), SlavBot.BOT.getGSON().toJson( log.getAsJsonObject() ).getBytes(), StandardOpenOption.WRITE );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    // This will repopulate the entire file. Note that this WILL construct the ENTIRE FILE into memory, due to loading all messages.
    // This uses an expensive call to IChannel#getFullMessageHistory()
    public void forceRepopulate()
    {
        SlavBot.BOT.getLogger().info( "Forcing repopulation for guild log " + guild.getName() + " (" + guild.getStringID() + ")" );
        init( false ); // WILL OVERRIDE FILE.
        for ( IChannel chan : guild.getChannels() )
            if ( chan.getModifiedPermissions( SlavBot.BOT.getBot().getOurUser() ).contains( Permissions.READ_MESSAGES ) )
            {
                for ( IMessage msg : chan.getFullMessageHistory() ) // this is VERY expensive.
                    logMessage( msg, false );
            }
        save();
    }
}
