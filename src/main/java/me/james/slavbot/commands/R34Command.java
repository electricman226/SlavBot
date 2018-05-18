package me.james.slavbot.commands;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import me.james.basebot.command.*;
import me.james.slavbot.*;
import org.apache.commons.io.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class R34Command extends Command
{
    private static String parseTags( String[] arr )
    {
        StringBuilder builder = new StringBuilder();
        for ( String s : arr )
            builder.append( s ).append( " " );
        return builder.toString().trim();
    }

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 2 )
            return null;
        ArrayList< String > tags = new ArrayList<>();
        ArrayList< String > notTags = new ArrayList<>();
        ArrayList< ImageURL > urls = new ArrayList<>();

        for ( int i = 1; i < args.length; i++ )
        {
            String tag = args[i];
            if ( tag.startsWith( "!" ) )
                notTags.add( tag.substring( 1 ) );
            else
                tags.add( tag );
        }

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet r34 = new HttpGet( String.format( "https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=%s&limit=200", parseTags( tags.toArray( new String[0] ) ) ).replace( ' ', '+' ) );
        try
        {
            HttpResponse resp = client.execute( r34 );
            String xml = IOUtils.toString( resp.getEntity().getContent(), StandardCharsets.UTF_8 );
            Document doc = Jsoup.parse( xml );
            for ( Element post : doc.getElementsByTag( "posts" ).get( 0 ).getElementsByTag( "post" ) )
            {
                boolean hasNot = false;
                for ( String t : notTags )
                    if ( post.attr( "tags" ).contains( t ) )
                    {
                        hasNot = true;
                        break;
                    }
                if ( hasNot )
                    continue;
                urls.add( new ImageURL( post.attr( "file_url" ), post.attr( "tags" ).split( " " ) ) );
            }
            if ( urls.size() <= 0 )
            {
                RequestBuffer.request( () -> chan.sendMessage( "No images with this tag found." ) );
                return "Tag not found.";
            }
            SlavBot.createViewer( urls.toArray( new ImageURL[0] ), chan );
        } catch ( IOException e )
        {
            SlavImageCommand.reportException( chan, e );
            e.printStackTrace();
        }
        return "Success, found " + urls.size() + " images, with the tags " + tags.toString() + ", and without the tags " + notTags.toString();
    }
}
