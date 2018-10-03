package me.james.slavbot;

import com.google.gson.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.sound.sampled.*;
import me.james.basebot.*;
import me.james.basebot.command.*;
import me.james.discord4click.*;
import me.james.slavbot.commands.*;
import me.james.slavbot.commands.operator.*;
import org.imgscalr.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.impl.events.guild.channel.*;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.handle.impl.events.guild.member.*;
import sx.blah.discord.handle.impl.events.guild.role.*;
import sx.blah.discord.handle.impl.events.guild.voice.*;
import sx.blah.discord.handle.impl.events.guild.voice.user.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import sx.blah.discord.util.audio.*;

public class SlavBot extends BaseBot
{
    public static final File ANCHOR_DIR = new File( "Slavbot-resources/anchors" );
    public static final File SOUNDS_DIR = new File( "Slavbot-resources/discordsounds" );
    public static SlavBot BOT;
    public static ArrayList< Command > anchorCmds = new ArrayList<>();
    public static ArrayList< String > soundList = new ArrayList<>();

    public SlavBot()
    {
        super( new File( "client_token" ) );
    }

    public static void main( String[] args )
    {
        BOT = new SlavBot();
    }

    public static void registerAnchors()
    {
        if ( ANCHOR_DIR.exists() )
        {
            anchorCmds.clear();
            for ( File f : ANCHOR_DIR.listFiles( ( f ) -> f.getName().endsWith( ".json" ) ) )
            {
                JsonObject json = fileToJSON( f );
                Command cmd;
                Command.registerCommand( "." + json.get( "command" ).getAsString(), ( cmd = new SlavImageCommand()
                {
                    @Override
                    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg, String imgUrl )
                    {
                        try
                        {
                            BufferedImage img = getImageFromUrl( imgUrl );
                            BufferedImage baseImg = ImageIO.read( new File( ANCHOR_DIR.getPath() + "/" + json.get( "file" ).getAsString() ) );
                            if ( json.get( "anchorType" ).getAsInt() == 0 ) // Overlay img over baseImg
                            {
                                int x, y, width, height, anchorType, scaleType;
                                x = json.get( "anchorX" ).getAsInt();
                                y = json.get( "anchorY" ).getAsInt();
                                width = json.get( "width" ).getAsInt();
                                height = json.get( "height" ).getAsInt();
                                scaleType = json.get( "scaleType" ).getAsInt();
                                anchorType = json.get( "anchorType" ).getAsInt();
                                if ( scaleType == 0 )
                                    baseImg.getGraphics().drawImage( img, x, y, width, height, null );
                                else if ( scaleType == 1 )
                                {
                                    BufferedImage lastImg = Scalr.resize( img, Math.min( width, height ) );
                                    baseImg.getGraphics().drawImage( lastImg, width / 4, height / 4, null );
                                }
                                File out = File.createTempFile( "sb_anchormsg_" + msg.getStringID(), "_img." + getExtension( imgUrl ) );
                                out.deleteOnExit();
                                ImageIO.write( baseImg, "png", out );
                                RequestBuffer.request( () -> {
                                    try
                                    {
                                        chan.sendFile( out );
                                    }
                                    catch ( FileNotFoundException e )
                                    {
                                        reportException( chan, e );
                                        e.printStackTrace();
                                    }
                                } );
                            }
                        }
                        catch ( IOException e )
                        {
                            reportException( chan, e );
                            e.printStackTrace();
                        }
                        return null;
                    }
                } ) );
                anchorCmds.add( cmd );
            }
        }
    }

    public static void registerSounds()
    {
        if ( SOUNDS_DIR.exists() )
        {
            soundList.clear();
            for ( File f : SOUNDS_DIR.listFiles( ( f ) -> f.getName().endsWith( ".wav" ) || f.getName().endsWith( ".mp3" ) ) )
            {
                String cmd = "." + f.getName().split( Pattern.quote( "." ) )[0];
                Command.registerCommand( cmd, new Command()
                {
                    @Override
                    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
                    {
                        JsonObject config = BOT.getConfig( chan.getGuild() );
                        if ( !config.has( "soundsChannel" ) || !( chan.getLongID() == config.get( "soundsChannel" ).getAsLong() ) )
                            return "No valid channel.";

                        if ( user.getVoiceStateForGuild( chan.getGuild() ).getChannel() == null )
                            return "Not in a channel.";

                        user.getVoiceStateForGuild( chan.getGuild() ).getChannel().join();
                        AudioPlayer player = AudioPlayer.getAudioPlayerForGuild( chan.getGuild() );
                        try
                        {
                            player.queue( f );
                        }
                        catch ( IOException | UnsupportedAudioFileException e )
                        {
                            e.printStackTrace();
                        }

                        return null;
                    }
                } );
                soundList.add( cmd );
            }
        }
    }

    public static void createViewer( ImageURL[] imgs, IChannel chan )
    {
        if ( imgs.length == 0 )
            throw new IllegalStateException( "No image URLs supplied for viewer!" );
        IMessage msg = chan.sendMessage( imgs[0].url + "\n" + Arrays.toString( imgs[0].tags ) );
        AtomicInteger count = new AtomicInteger( 0 );
        Discord4Click.addClickEvent( msg, "\u25C0", user -> {
            if ( count.get() == 0 )
                return null;
            count.decrementAndGet();
            RequestBuffer.request( () -> msg.edit( imgs[count.get()].url + "\n" + Arrays.toString( imgs[count.get()].tags ) ) );
            return null;
        }, true );

        Discord4Click.addClickEvent( msg, "\u25B6", user -> {
            if ( count.get() >= imgs.length - 1 )
                return null;
            count.incrementAndGet();
            RequestBuffer.request( () -> msg.edit( imgs[count.get()].url + "\n" + Arrays.toString( imgs[count.get()].tags ) ) );
            return null;
        }, true );

        Discord4Click.addClickEvent( msg, "\u23F9", user -> {
            RequestBuffer.request( msg::delete );
            return null;
        } );
    }

    @EventSubscriber
    public void onCreateChannel( ChannelCreateEvent e )
    {
        GuildLog.getLogForGuild( e.getGuild() ).logChannel( e.getChannel() );
    }

    @EventSubscriber
    public void onUserGuildJoin( UserJoinEvent e )
    {
        JsonObject config;
        if ( ( config = getConfig( e.getGuild() ) ) != null && config.has( "defaultRole" ) )
        {
            ArrayList< IRole > roles = new ArrayList<>();
            for ( JsonElement elem : config.getAsJsonArray( "defaultRole" ) )
                roles.add( e.getGuild().getRoleByID( elem.getAsLong() ) );
            e.getGuild().editUserRoles( e.getUser(), roles.toArray( new IRole[0] ) );
        }
    }

    @EventSubscriber
    public void onRoleEvent( RoleEvent e )
    {
        if ( e instanceof RoleCreateEvent )
            getLogger().info( "Role " + e.getRole().getName() + "/" + e.getRole().getStringID() + " created." );
        if ( e instanceof RoleUpdateEvent )
            getLogger().info( "Role " + ( (RoleUpdateEvent) e ).getOldRole().getName() + "/" + ( (RoleUpdateEvent) e ).getOldRole().getStringID() + " (now " + ( (RoleUpdateEvent) e ).getNewRole().getName() + "/" + ( (RoleUpdateEvent) e ).getNewRole().getStringID() + ") updated." );
    }

    @EventSubscriber
    public void onUserRoleEvent( UserRoleUpdateEvent e )
    {
        StringBuilder builder = new StringBuilder( "User " + e.getUser().getName() + "/" + e.getUser().getStringID() + " roles updated:\n\tAdded: " );
        for ( IRole r : e.getNewRoles() )
            if ( !e.getOldRoles().contains( r ) )
                builder.append( r.getName() ).append( "/" ).append( r.getStringID() ).append( ", " );
        builder = new StringBuilder( builder.subSequence( 0, builder.length() - 2 ) ).append( "\n\tRemoved: " );

        for ( IRole r : e.getOldRoles() )
            if ( !e.getNewRoles().contains( r ) )
                builder.append( r.getName() ).append( "/" ).append( r.getStringID() ).append( ", " );
        getLogger().info( builder.toString() );
    }

    @EventSubscriber
    public void onVoiceEvent( VoiceChannelEvent e )
    {
        IVoiceChannel ourChan = e.getClient().getOurUser().getVoiceStateForGuild( e.getGuild() ).getChannel();
        if ( ourChan == null )
            return;
        JsonObject conf = getConfig( e.getGuild() );
        if ( !conf.has( "voiceStateSounds" ) || !conf.get( "voiceStateSounds" ).getAsJsonObject().has( "joinSound" ) || !conf.get( "voiceStateSounds" ).getAsJsonObject().has( "leaveSound" ) || !conf.get( "voiceStateSounds" ).getAsJsonObject().has( "joinSoundDelay" ) || !conf.get( "voiceStateSounds" ).getAsJsonObject().has( "userCountSounds" ) )
            return;
        File joinSound = new File( conf.get( "voiceStateSounds" ).getAsJsonObject().get( "joinSound" ).getAsString() );
        File leaveSound = new File( conf.get( "voiceStateSounds" ).getAsJsonObject().get( "leaveSound" ).getAsString() );
        long delay = conf.get( "voiceStateSounds" ).getAsJsonObject().get( "joinSoundDelay" ).getAsLong();
        JsonArray userCountSounds = conf.get( "voiceStateSounds" ).getAsJsonObject().get( "userCountSounds" ).getAsJsonArray();

        if ( e instanceof UserVoiceChannelMoveEvent || e instanceof UserVoiceChannelJoinEvent || e instanceof UserVoiceChannelLeaveEvent )
        {
            IUser user;
            if ( e instanceof UserVoiceChannelMoveEvent )
                user = ( (UserVoiceChannelMoveEvent) e ).getUser();
            else if ( e instanceof UserVoiceChannelJoinEvent )
                user = ( (UserVoiceChannelJoinEvent) e ).getUser();
            else
                user = ( (UserVoiceChannelLeaveEvent) e ).getUser();
            if ( user == e.getClient().getOurUser() )
                return;
            if ( !( e instanceof UserVoiceChannelLeaveEvent ) && e.getVoiceChannel() == ourChan )
            {
                // Join sound.
                new Thread( () -> {
                    try
                    {
                        Thread.sleep( delay );
                        AudioPlayer.getAudioPlayerForGuild( e.getGuild() ).queue( joinSound );

                        int count = e.getVoiceChannel().getConnectedUsers().size();
                        for ( JsonElement elem : userCountSounds )
                        {
                            JsonObject obj = elem.getAsJsonObject();
                            if ( obj.get( "count" ).getAsInt() == count )
                            {
                                File f = new File( obj.get( "sound" ).getAsString() );
                                if ( !f.exists() )
                                    continue;
                                try
                                {
                                    AudioPlayer.getAudioPlayerForGuild( e.getGuild() ).queue( f );
                                }
                                catch ( IOException | UnsupportedAudioFileException e1 )
                                {
                                    e1.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                    catch ( InterruptedException | IOException | UnsupportedAudioFileException e1 )
                    {
                        e1.printStackTrace();
                    }
                } ).start();

                return;
            }
            if ( e instanceof UserVoiceChannelLeaveEvent || e instanceof UserVoiceChannelMoveEvent && e.getVoiceChannel() != ourChan )
            {
                // Leave sound.
                try
                {
                    AudioPlayer.getAudioPlayerForGuild( e.getGuild() ).queue( leaveSound );
                }
                catch ( IOException | UnsupportedAudioFileException e1 )
                {
                    e1.printStackTrace();
                }
                return;
            }
        }
    }

    @EventSubscriber
    public void onMessage( MessageEvent e )
    {
        if ( e instanceof MessageReceivedEvent && !e.getChannel().isPrivate() )
            GuildLog.getLogForGuild( e.getGuild() ).logMessage( e.getMessage() );

        if ( e instanceof MessageSendEvent || e instanceof MessageReceivedEvent )
            if ( !e.getChannel().isPrivate() && SlavImageCommand.hasImage( e.getMessage() ) )
                SlavImageCommand.lastMsgs.put( e.getChannel(), e.getMessage() );
    }

    @Override
    public void init()
    {
        getBot().getDispatcher().registerListener( new Discord4Click() );
        Discord4Click.init();
        Command.registerCommand( ".jpeg", new JPEGCommand() );
        Command.registerCommand( ".e", new EmojiCommand() );
        Command.registerCommand( ".deepfry", new DeepfryCommand() );
        Command.registerCommand( ".reloadanchors", new ReloadAnchorsCommand() );
        Command.registerCommand( ".sounds", new ListSoundsCommand() );
        Command.registerCommand( ".r34", new R34Command() );
        Command.registerCommand( ".clearsnd", new ClearSoundQueueCommand() );
        Command.registerCommand( ".?", new RandomRedditCommand() );
        Command.registerCommand( ".muteall", new MuteAllCommand() );
        Command.registerCommand( ".deafenall", new DeafenAllCommand() );
        Command.registerCommand( ".mutechan", new TempMuteChannelCommand() );
        Command.registerCommand( ".sendmsg", new SendMessageCommand() );

        registerAnchors();
        registerSounds();

        for ( IGuild g : getBot().getGuilds() )
            GuildLog.create( g );

        new RuntimeCompiler().start();
    }
}
