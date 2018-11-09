package me.james.slavbot.commands;

import java.io.*;
import javax.sound.sampled.*;
import me.james.basebot.command.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.audio.*;

public class RawSoundCommand extends Command
{
    public static final File RAW_SOUND_DIR = new File( "rawsound" );

    static
    {
        if ( !RAW_SOUND_DIR.exists() )
            RAW_SOUND_DIR.mkdir();
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 2 )
            return "Not enough args";

        String snd = args[1];
        File sndFile = new File( RAW_SOUND_DIR.getPath() + "/" + snd );
        if ( !sndFile.exists() )
            return "Invalid sound";
        
        if ( user.getVoiceStateForGuild( chan.getGuild() ).getChannel() == null )
            return "Not in a channel.";
        user.getVoiceStateForGuild( chan.getGuild() ).getChannel().join();
        AudioPlayer player = AudioPlayer.getAudioPlayerForGuild( chan.getGuild() );
        try
        {
            player.queue( sndFile );
        }
        catch ( IOException | UnsupportedAudioFileException e )
        {
            e.printStackTrace();
        }
        return null;
    }
}
