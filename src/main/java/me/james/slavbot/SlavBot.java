package me.james.slavbot;

import com.google.gson.*;
import java.awt.image.*;
import java.io.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.sound.sampled.*;
import me.james.basebot.*;
import me.james.basebot.command.*;
import me.james.slavbot.commands.*;
import org.imgscalr.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import sx.blah.discord.util.audio.*;

public class SlavBot extends BaseBot
{
    public static final File ANCHOR_DIR = new File( "anchors" );
    public static final File SOUNDS_DIR = new File( "discordsounds" );
    public static Thread vcLeaveThrd;
    public static SlavBot BOT;

    public SlavBot()
    {
        super( new File( "client_token" ) );
    }

    public static void main( String[] args )
    {
        BOT = new SlavBot();
    }

    @EventSubscriber
    public void onMessageSend( MessageSendEvent e )
    {
        if ( !e.getChannel().isPrivate() && SlavImageCommand.hasImage( e.getMessage() ) )
            SlavImageCommand.lastMsgs.put( e.getChannel(), e.getMessage() );
    }

    @Override
    @EventSubscriber
    public void onMessage( MessageReceivedEvent e )
    {
        super.onMessage( e );
        if ( SlavImageCommand.hasImage( e.getMessage() ) )
            SlavImageCommand.lastMsgs.put( e.getChannel(), e.getMessage() );
    }

    @Override
    public void init()
    {
        Command.registerClass( ".jpeg", new JPEGCommand() );
        Command.registerClass( ".e", new EmojiCommand() );
        Command.registerClass( ".deepfry", new DeepfryCommand() );

        if ( ANCHOR_DIR.exists() )
            for ( File f : ANCHOR_DIR.listFiles( ( f ) -> f.getName().endsWith( ".json" ) ) )
            {
                JsonObject json = fileToJSON( f );
                Command.registerClass( "." + json.get( "command" ).getAsString(), new SlavImageCommand()
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
                } );
            }

        if ( SOUNDS_DIR.exists() )
            for ( File f : SOUNDS_DIR.listFiles( ( f ) -> f.getName().endsWith( ".wav" ) || f.getName().endsWith( ".mp3" ) ) )
            {
                Command.registerClass( "." + f.getName().split( Pattern.quote( "." ) )[0], new Command()
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
            }
    }
}
