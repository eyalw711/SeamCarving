import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SeamCarving 
{

	public static void main(String[] args) 
	{
		BufferedImage img = null;
		try 
		{
		    img = ImageIO.read(new File("strawberry.jpg"));
		} 
		catch (IOException e) 
		{
			
		}
		
		System.out.println("Height = " + img.getHeight() + " Width = " + img.getWidth());
	}

}
