package me.james.slavbot;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import sx.blah.discord.handle.obj.*;

public class GuildLog
{
    public static final File LOG_DIRECTORY = new File( "guildlogs" );
    public static HashMap< IGuild, GuildLog > LOGS = new HashMap<>();

    static
    {
        if ( !LOG_DIRECTORY.exists() )
            LOG_DIRECTORY.mkdir();
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
        JsonArray overallPermissions = new JsonArray();
        msgObj.add( "overall_permissions", overallPermissions );
        msgObj.addProperty( "overall_permissions_bits", Permissions.generatePermissionsNumber( msg.getAuthor().getPermissionsForGuild( msg.getGuild() ) ) );

        messages.add( msgObj );
        if ( save )
            save();
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
