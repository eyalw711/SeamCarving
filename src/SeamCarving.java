import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SeamCarving 
{
	static final int VERTICAL = 0;
	static final int VERTICAL_SEAM = 1;
	static final int HORIZONTAL = 2;
	static final int HORIZONTAL_SEAM = 3;

	static final int ENERGY_REGULAR = 0;
	static final int ENERGY_ENTROPY = 1;
	static final int ENERGY_FORWARD = 2;

	public static void main(String[] args) 
	{
		//Get input command line arguments
        int outCols;//600 is the width of source cats.jpg
        int outRows;//366 is the height of source cats.jpg
        String inputImageFilename;// = "cats.jpg" "halong_bay.jpg" "strawberry.jpg"
        String outputImageFilename;// = "cats_result.jpg" "halong_bay_result.jpg" "strawberry_result.jpg"
        int energyType;// = ENERGY_REGULAR;

        if (args.length != 5){
            System.out.println("ERROR: got " + args.length + " arguments while expecting 5 arguments. exting...");
            return;
        }else{
            inputImageFilename=args[0];
            outCols=Integer.parseInt(args[1]);
            outRows=Integer.parseInt(args[2]);
            energyType=Integer.parseInt(args[3]);
            outputImageFilename=args[4];
        }

		try 
		{
			//Read image from input file
			BufferedImage img = null;
			img = ImageIO.read(new File(inputImageFilename));

			//Get image dimensions
		    WritableRaster rast = img.getRaster();
		    int rows = rast.getHeight();
		    int cols = rast.getWidth();

		    //Calculate delta columns & delta rows required
			int deltaRows = rows - outRows;
			int deltaCols = cols - outCols;

			//Set pixels array out of source image
		    int[][] pixels = new int[rows][cols];
		    imageTo2DPixelsArray(img, rows, cols, pixels);

			//Remove horizontal seams
			int[][] pixelsRowsSeamed = (deltaRows == 0) ? pixels :
													  	removeOrAddSeams(rows, cols, deltaRows, pixels, HORIZONTAL_SEAM, energyType);
			//Remove vertical seams
			int[][] pixelsResult = (deltaCols == 0) ? pixelsRowsSeamed :
													  removeOrAddSeams(outRows, cols, deltaCols, pixelsRowsSeamed, VERTICAL_SEAM, energyType);

		    //Set result image
			BufferedImage resultImage = null;
			resultImage = new BufferedImage(outCols, outRows, BufferedImage.TYPE_INT_RGB);
		    for (int i = 0; i < outRows; i++) {
		    	for (int j = 0; j < outCols; j++) {
		    		resultImage.setRGB(j, i, pixelsResult[i][j]);
		    	}
		    }
			ImageIO.write(resultImage, "jpg", new File(outputImageFilename));

		    System.out.println("Source image Height = " + img.getHeight() + ", Width = " + img.getWidth());
			System.out.println("Result image Height = " + resultImage.getHeight() + ", Width = " + resultImage.getWidth());
		} 
		catch (IOException e) 
		{
			System.out.println("Caught exception" + e.toString());
		}
	}

	private static void imageTo2DPixelsArray(BufferedImage img, int rows, int cols, int[][] pixels) 
	{
		for( int i = 0; i < rows; i++ )
		    for( int j = 0; j < cols; j++ )
		        pixels[i][j] = img.getRGB( j, i );
	}

	private static int[][] removeOrAddSeams(int rows, int cols, int delta, int[][] pixels, int mode, int energyType)
	{
		int[][] pixelsTrans = new int[cols][rows];
		boolean transpose = false;
		//Transpose matrix if needed
		if (mode == HORIZONTAL || mode == HORIZONTAL_SEAM){
			//Set transposed matrix
			for (int i = 0 ; i < rows ; i++){
				for(int j = 0 ; j < cols ; j++){
					pixelsTrans[j][i] = pixels[i][j];
				}
			}
			//Swap cols<->rows
			int temp = cols;
			cols = rows;
			rows = temp;

			//Change mode from HORIZONTAL_SEAM/HORIZONTAL to VERTICAL_SEAM/VERTICAL
			mode = (mode == HORIZONTAL_SEAM) ? VERTICAL_SEAM : VERTICAL;

			transpose = true;
		}

		//Set work pixels matrix
		int[][] workPixels = (transpose) ? pixelsTrans : pixels;

		//Set absolute delta and carveIn/carveOut boolean variable
		int absDelta = Math.abs(delta);
		boolean is_in = (delta < 0);

		while (absDelta > 0)
		{
			//Calculate energy map & dynamic programming results
			int[][] energyMap = (energyType == ENERGY_FORWARD) ?
									SeamCarving.energyFunctionFor(rows, cols, workPixels) :
									(energyType == ENERGY_ENTROPY) ?
											SeamCarving.energyFunctionEnt(rows, cols, workPixels) :
											SeamCarving.energyFunctionReg(rows, cols, workPixels);

			int[][] dynProgResult = SeamCarving.dynamicProgrammingSumEnergy(rows, cols, energyMap, mode);

			//Calculate one seam
			Seam s = new Seam(rows);
			s.form(dynProgResult, mode);

			//Carve seam in/out
			int[][] carveSeamPixels = (is_in) ? carveInSeam(workPixels, rows, cols + 1, s) :
												 carveOutSeam(workPixels, rows, cols - 1, s);
			workPixels = carveSeamPixels;

			//Update abs Delta & columns
			absDelta--;
			if (is_in){
				cols++;
			}else{
				cols--;
			}
		}

		//Transpose matrix back if needed
		if (transpose){
			int[][] pixelsResult = new int[cols][rows];
			for (int i = 0 ; i < rows ; i++){
				for(int j = 0 ; j < cols ; j++){
					pixelsResult[j][i] = workPixels[i][j];
				}
			}
			return pixelsResult;
		}

		return workPixels;
	}

	private static int[][] energyFunctionReg(int rows, int cols, int[][] pixels)
	{
		int[][] energyMap = new int[rows][cols];
		for( int i = 0; i < rows; i++ )
			for( int j = 0; j < cols; j++ )
				energyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return energyMap;
	}

	private static int[][] energyFunctionEnt(int rows, int cols, int[][] pixels)
	{
		//TODO: IMPLEMENT. right now - same as energyFunctionReg.
		int[][] energyMap = new int[rows][cols];
		for( int i = 0; i < rows; i++ )
			for( int j = 0; j < cols; j++ )
				energyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return energyMap;
	}

	private static int[][] energyFunctionFor(int rows, int cols, int[][] pixels)
	{
		//TODO: IMPLEMENT. right now - same as energyFunctionReg.
		int[][] energyMap = new int[rows][cols];
		for( int i = 0; i < rows; i++ )
			for( int j = 0; j < cols; j++ )
				energyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return energyMap;
	}

	private static int ColorsGradient(int i, int j, int rows, int cols, int[][] pixels)
	{
		int gradient = 0;
		int neighbors = 0;

		int ii = - 1, jj = -1, iimax = 2,  jjmax = 2;
		if ( i == 0 || i == rows - 1 || j == 0 || j == cols - 1)
		{
			//special case: try "ignoring" edges:
			if (i == 0)
				ii = 0;
			else if (i == rows - 1)
				iimax = 1;

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

				neighbors++;

				n = new Color(pixels[i+ii][j+jj]);
				delta_red = Math.abs(red - n.getRed());
				delta_green = Math.abs(green - n.getGreen());
				delta_blue = Math.abs(blue - n.getBlue());

				gradient += ((delta_red + delta_green + delta_blue) / 3);
			}
		}
		return (gradient/neighbors);
	}

	private static int[][] dynamicProgrammingSumEnergy(int rows, int cols, int[][] energyMap, int mode)
	{
		int[][] dynProgResult = new int[rows][cols];
		boolean[] lookAt = new boolean[3];
		if (mode == VERTICAL)
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
				if ( j == 0 || j == cols - 1 && mode == VERTICAL_SEAM)
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

	private static int[][] carveInSeam(int[][] pixels, int numOfRows, int newNumOfCols, Seam s)
	{
		int[][] newPixels = new int[numOfRows][newNumOfCols];
		//TODO: IMPLEMENT
		return newPixels;
	}


	private static int[][] paintSeam(int[][]pixelsSeams, int numOfRows, Seam s)//for DEBUG only
	{
		for (int row = 0; row < numOfRows; row++)
		{
			pixelsSeams[row][s.cols[row]] = 0;
		}
		return pixelsSeams;
	}
}

//			int[][] pixelsSeams = pixels;//for DEBUG only

//			//Original image with selected seams - for DEBUG only
//			BufferedImage seamImage = null;
//			int origRows = rast.getHeight();
//			int origCols = rast.getWidth();
//			seamImage = new BufferedImage(origCols, origRows, BufferedImage.TYPE_INT_RGB);
//			for (int i = 0; i < origRows; i++)
//			{
//				for (int j = 0; j < origCols; j++)
//				{
//					seamImage.setRGB(j, i, pixelsSeams[i][j]);
//				}
//			}
//
//			//ImageIO.write(seamImage, "jpg", new File("strawberry_seams_result_vertical_100.jpg"));
//			ImageIO.write(seamImage, "jpg", new File("cats_seams_result_horizontal_seam_100.jpg"));
//			//ImageIO.write(seamImage, "jpg", new File("halong_bay_seams_result.jpg"));
