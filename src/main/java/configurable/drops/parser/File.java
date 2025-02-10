package configurable.drops.parser;

import configurable.drops.ConfigurableDrops;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

//excellent explanation of File readers https://stackoverflow.com/questions/9648811/specific-difference-between-bufferedreader-and-filereader
public class File
{
    protected final java.io.File directory = new java.io.File("config/" + ConfigurableDrops.modId);

    public File()
    {
        this.directory.mkdir();
    }

    public static void closeReader(Reader reader)
    {
        try
        {
            reader.close();
        }
        catch(IOException e)
        {
            ConfigurableDrops.logger.error(e.getMessage(),e.getCause());
        }
    }

    public Reader getFileReader(String fileName)
    {
        try
        {
            java.io.File file = new java.io.File(this.directory, fileName);
            file.createNewFile();
            Reader inputStream = new FileReader(file);
            return inputStream;
        }
        catch(SecurityException e)
        {
            ConfigurableDrops.logger.error(e.getMessage(),e.getCause());
        }
        catch(FileNotFoundException e)
        {
            ConfigurableDrops.logger.error(e.getMessage(),e.getCause());
        }
        catch(IOException e)
        {
            ConfigurableDrops.logger.error(e.getMessage(),e.getCause());
        }
        return null;
    }
}
