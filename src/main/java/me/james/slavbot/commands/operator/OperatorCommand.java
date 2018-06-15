package me.james.slavbot.commands.operator;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;

public abstract class OperatorCommand extends Command
{
    public static IRole getOperatorRole( IGuild guild )
    {
        if ( !SlavBot.BOT.getConfig( guild ).has( "operatorRole" ) )
            return null;
        return guild.getRoleByID( SlavBot.BOT.getConfig( guild ).get( "operatorRole" ).getAsLong() );
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( !SlavBot.BOT.getConfig( chan.getGuild() ).has( "operatorChannel" ) || chan.getLongID() != SlavBot.BOT.getConfig( chan.getGuild() ).get( "operatorChannel" ).getAsLong() )
            return "Not an operator channel.";
        IRole operator = getOperatorRole( chan.getGuild() );
        if ( operator == null )
            return "No operator role? (is null)";

        if ( user.getRolesForGuild( chan.getGuild() ).stream().anyMatch( role -> role.getLongID() == operator.getLongID() ) )
            return doOperatorCommand( args, user, chan, msg );

        return "User was not an operator.";
    }

    public abstract String doOperatorCommand( String[] args, IUser user, IChannel chan, IMessage msg );
}
