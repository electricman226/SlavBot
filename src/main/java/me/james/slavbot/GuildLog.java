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
        log = new JsonObject();
        JsonObject guild_info = new JsonObject();
        guild_info.addProperty( "name", guild.getName() );
        // TODO: Add more guild specific info.
        // FIXME: What if the name changes? What about other guild information?
        log.add( "guild_info", guild_info );
        log.add( "messages", new JsonArray() );
        save();
    }

    public void logMessage( IMessage msg )
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
}
