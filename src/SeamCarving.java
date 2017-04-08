import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class SeamCarving 
{
	static final int STRAIGHT = 0;
	static final int SEAM = 1;
	
	public static void main(String[] args) 
	{
		BufferedImage img = null;
		int k = 50;
		try 
		{
		    img = ImageIO.read(new File("cats.jpg"));
		    
		    WritableRaster rast = img.getRaster();
		    int rows = rast.getHeight();
		    int cols = rast.getWidth();
		    int[][] pixels = new int[rows][cols];
		    
		    imageTo2DPixelsArray(img, rows, cols, pixels);
		    
		    while (k > 0)
		    {
		    	int[][] energyMap = SeamCarving.energyFunction1(rows, cols, pixels);
			    int[][] dynProgResult = dynamicProgrammingSumEnergy(rows, cols, energyMap, STRAIGHT); 

			    Seam s = new Seam(rows);
			    s.form(dynProgResult, STRAIGHT);
			    
			    int[][] carveOutSeamPixels = carveOutSeam(pixels, rows, cols - 1, s);
			    pixels = carveOutSeamPixels;
			    k--;
			    cols--;
		    }
		    
		    BufferedImage resultImage = null;
		    resultImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
		    for (int i = 0; i < rows; i++)
		    {
		    	for (int j = 0; j < cols; j++)
		    	{
		    		resultImage.setRGB(j, i, pixels[i][j]);		    		
		    	}
		    }
		    
		    ImageIO.write(resultImage, "jpg", new File("catsresult.jpg"));

		    System.out.println("Height = " + img.getHeight() + " Width = " + img.getWidth());
		} 
		catch (IOException e) 
		{
			System.out.println("Caught exception" + e.toString());
		}
		
		
	}

	private static int[][] carveOutSeam(int[][] pixels, int numOfRows, int newNumOfCols, Seam s) 
	{
		int[][] newPixels = new int[numOfRows][newNumOfCols];
		for (int row = 0; row < numOfRows; row++)
		{
			int skipIndex = s.cols[row];
			System.arraycopy(pixels[row], 0, newPixels[row], 0, skipIndex);
			System.arraycopy(pixels[row], skipIndex + 1, newPixels[row], skipIndex, newNumOfCols - skipIndex);
		}
		return newPixels;
	}

	private static void imageTo2DPixelsArray(BufferedImage img, int rows, int cols, int[][] pixels) 
	{
		for( int i = 0; i < rows; i++ )
		    for( int j = 0; j < cols; j++ )
		        pixels[i][j] = img.getRGB( j, i );
	}

	private static int[][] dynamicProgrammingSumEnergy(int rows, int cols, int[][] energyMap, int mode)
	{
		int[][] dynProgResult = new int[rows][cols];
		boolean[] lookAt = new boolean[3];
		if (mode == STRAIGHT)
		{
			lookAt[0] = false;
			lookAt[1] = true;
			lookAt[2] = false;
		}
		else
		{
			lookAt[0] = lookAt[1] = lookAt[2] = true;
		}
		
		for (int i = 1; i < rows; i++)
		{
			for (int j = 0; j < cols; j++)
			{
				if ( j == 0 || j == cols - 1 && mode == SEAM)
				{
					if ( j == 0)
					{
						lookAt[0] = false;
						dynamicProgrammingStep(energyMap, dynProgResult, lookAt, i, j);
						lookAt[0] = true;
					}
					else
					{
						lookAt[2] = false;
						dynamicProgrammingStep(energyMap, dynProgResult, lookAt, i, j);
						lookAt[2] = true;
					}
				}
				else
				{
					dynamicProgrammingStep(energyMap, dynProgResult, lookAt, i, j);
				}
			}
		}
		return dynProgResult;
	}

	private static void dynamicProgrammingStep(int[][] energyMap, int[][] dynProgResult, boolean[] lookAt, int i,
			int j) 
	{
		int minimalValueFound = Integer.MAX_VALUE;
		for (int k = 0; k < 3; k++)
		{
			if (lookAt[k] && dynProgResult[i-1][j + k - 1] < minimalValueFound)
				minimalValueFound = dynProgResult[i-1][j + k - 1];
		}
		dynProgResult[i][j] = energyMap[i][j] + minimalValueFound;
	}

	private static int[][] energyFunction1(int rows, int cols, int[][] pixels) 
	{
		int[][] energyMap = new int[rows][cols];
		for( int i = 0; i < rows; i++ )
	        for( int j = 0; j < cols; j++ )
	            energyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return energyMap;
	}

	private static int ColorsGradient(int i, int j, int rows, int cols, int[][] pixels) 
	{
		int gradient = 0;
		int ii = - 1, jj = -1, iimax = 2,  jjmax = 2;
		if ( i == 0 || i == rows - 1 || j == 0 || j == cols - 1)
		{
			//special case: try "ignoring" edges:
			
			if (i == 0)
				ii = 0;
			else if (i == rows - 1)
				iimax = 0;
			
			if (j == 0)
				jj = 0;
			else if (j == cols - 1)
				jjmax = 1;			
		}

		Color c = new Color(pixels[i][j]);
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		
		Color n = null;
		int delta_red, delta_green, delta_blue;
		for ( ; ii < iimax; ii++)
		{
			for ( ; jj < jjmax; jj++)
			{
				if (ii==0 && jj==0)
					continue;
				
				n = new Color(pixels[i+ii][j+jj]);
				delta_red = red - n.getRed();
				delta_green = green - n.getGreen();
				delta_blue = blue - n.getBlue();
				
				gradient += (delta_red + delta_green + delta_blue);
			}
		}
		return gradient;
	}
}
