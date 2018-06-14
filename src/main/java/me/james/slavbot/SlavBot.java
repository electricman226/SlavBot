package me.james.slavbot;

import com.google.gson.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.sound.sampled.*;
import me.james.basebot.*;
import me.james.basebot.command.*;
import me.james.discord4click.*;
import me.james.slavbot.commands.*;
import org.imgscalr.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.handle.impl.events.guild.member.*;
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
                                    } catch ( FileNotFoundException e )
                                    {
                                        reportException( chan, e );
                                        e.printStackTrace();
                                    }
                                } );
                            }
                        } catch ( IOException e )
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
                        if ( !config.has( "soundsChannel" ) || !( chan.getStringID().equals( config.get( "soundsChannel" ).getAsString() ) ) )
                            return "No valid channel.";

                        if ( user.getVoiceStateForGuild( chan.getGuild() ).getChannel() == null )
                            return "Not in a channel.";

                        user.getVoiceStateForGuild( chan.getGuild() ).getChannel().join();
                        AudioPlayer player = AudioPlayer.getAudioPlayerForGuild( chan.getGuild() );
                        try
                        {
                            player.queue( f );
                        } catch ( IOException | UnsupportedAudioFileException e )
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
        int[] ind = new int[1]; // uhhh.
        Discord4Click.addClickEvent( msg, "\u25C0", user -> {
            if ( ind[0] == 0 )
                return null;
            ind[0] = ind[0] - 1;
            RequestBuffer.request( () -> msg.edit( imgs[ind[0]].url + "\n" + Arrays.toString( imgs[ind[0]].tags ) ) );
            return null;
        }, true );

        Discord4Click.addClickEvent( msg, "\u25B6", user -> {
            if ( ind[0] >= imgs.length - 1 )
                return null;
            ind[0] = ind[0] + 1;
            RequestBuffer.request( () -> msg.edit( imgs[ind[0]].url + "\n" + Arrays.toString( imgs[ind[0]].tags ) ) );
            return null;
        }, true );

        Discord4Click.addClickEvent( msg, "\u23F9", user -> {
            RequestBuffer.request( msg::delete );
            return null;
        } );
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
    public void onMessage( MessageEvent e )
    {
        if ( e instanceof MessageSendEvent || e instanceof MessageReceivedEvent )
        {
            if ( !e.getChannel().isPrivate() && SlavImageCommand.hasImage( e.getMessage() ) )
                SlavImageCommand.lastMsgs.put( e.getChannel(), e.getMessage() );
        }
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
        Command.registerCommand( ".?", new RandomRedditCommand() );

        registerAnchors();
        registerSounds();
    }
}
