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
		//Get start time (for running time measurement)
		long startTime = System.currentTimeMillis();

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
			BufferedImage img;
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
			BufferedImage resultImage;
			resultImage = new BufferedImage(outCols, outRows, BufferedImage.TYPE_INT_RGB);
		    for (int i = 0; i < outRows; i++) {
		    	for (int j = 0; j < outCols; j++) {
		    		resultImage.setRGB(j, i, pixelsResult[i][j]);
		    	}
		    }
			ImageIO.write(resultImage, "jpg", new File(outputImageFilename));

		    System.out.println("Source image "+ inputImageFilename + ": Height = " + img.getHeight() + ", Width = " + img.getWidth());
			System.out.println("Result image "+ outputImageFilename +": Height = " + resultImage.getHeight() + ", Width = " + resultImage.getWidth());
		} 
		catch (IOException e) 
		{
			System.out.println("Caught exception" + e.toString());
		}

		//Print running time message
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("running time = " + totalTime/1000 + " seconds");
	}

	private static void imageTo2DPixelsArray(BufferedImage img, int rows, int cols, int[][] pixels) 
	{
		for( int i = 0; i < rows; i++ )
		    for( int j = 0; j < cols; j++ )
		        pixels[i][j] = img.getRGB( j, i );
	}

	private static int[][] removeOrAddSeams(int rows, int cols, int delta, int[][] pixels, int mode, int energyType)
	{
		//Transpose matrix if needed
		int[][] pixelsTrans = new int[cols][rows];
		boolean transpose = false;
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
		boolean isEnlarge = (delta < 0);

		//Enlarge more than image size - first modification
		if (isEnlarge){
			//In case of enlarging the image more than its size - do it in steps: first stretch (equivalent to adding seams),
			// 																	  when absDelta<cols - add seams.
			while (absDelta >= cols){
				for (int i=0 ; i < cols ; i++){
					//create straight seam
					int[] colsArray = new int[rows];
					java.util.Arrays.fill(colsArray, i*2);
					Seam sStraight = new Seam(colsArray, rows);
					//insert new straight seam
					workPixels = carveInSeam(workPixels, rows, cols + i + 1, sStraight);
				}
				absDelta -= cols;
				cols *= 2;
			}
		}

		//save a copy of original workPixels & absDelta for enlarging case
		int[][] origWorkPixels = workPixels;
		int origAbsDelta = absDelta;

		//Create Seam array for enlarging case
		Seam[] dupSeams = new Seam[cols];
		for (int i=0 ; i < cols ; i++){
			dupSeams[i] = null;
		}

		//Create original indexes matrix for enlarging case
		int[][] origIndexes = new int[rows][cols];
		for (int i = 0 ; i < rows ; i++){
			for(int j = 0 ; j < cols ; j++){
				origIndexes[i][j] = j;
			}
		}

		//Help variables
		int[][] energyMap = new int[rows][cols];
		Seam s = null;
		int jStart = 0;
		int jEnd = cols;

		//Remove absDelta seams (in case of enlarging - will save the removed seams and add them to the original image)
		while (absDelta > 0) {
			//Calculate energy map results
			if (s != null){
				jStart = s.min-1;
				jEnd = s.max+2;
			}

			energyMap = (energyType == ENERGY_FORWARD) ?
					SeamCarving.energyFunctionFor(rows, cols, jStart, jEnd, workPixels, energyMap) :
					(energyType == ENERGY_ENTROPY) ?
							SeamCarving.energyFunctionEnt(rows, cols, jStart-4, jEnd+4, workPixels, energyMap) :
							SeamCarving.energyFunctionReg(rows, cols, jStart, jEnd, workPixels, energyMap);

			//Calculate dynamic programming results
			int[][] dynProgResult = SeamCarving.dynamicProgrammingSumEnergy(rows, cols, energyMap, mode);

			//Calculate one seam
			s = new Seam(rows);
			s.form(dynProgResult, mode);

			//In case of enlarging - save s (with original indexes) in dupSeams
			if (isEnlarge){
				Seam sOrigInd = new Seam(rows, s, origIndexes);
				dupSeams[sOrigInd.cols[rows-1]] = sOrigInd;
				//Carve seam out (from origIndexes matrix)
				origIndexes = carveOutSeam(origIndexes, rows, cols - 1, s);
			}

			//Carve seam out
			workPixels = carveOutSeam(workPixels, rows, cols - 1, s);
			energyMap = carveOutSeam(energyMap, rows, cols - 1, s);

			//Update abs Delta & columns
			absDelta--;
			cols--;
		}

		//In case of enlarging the image - enlarge image by saved seams
		if (isEnlarge){
			//Return cols to original size
			cols += origAbsDelta;

			//Create bool matrix of original size, cell=true if it should be duplicated
			//Create the matrix, initialize with false
			boolean[][] isDup = new boolean[rows][cols];
			//Translate seams to true value in isDup matrix
			for (int i = 0 ; i < cols ; i++){
				if (dupSeams[i] != null){
					for (int j = 0 ; j < dupSeams[i].rows ; j++){
						isDup[j][dupSeams[i].cols[j]] = true;
					}
				}
			}
			//Create result matrix using isDup matrix
			workPixels = duplicateCells(origWorkPixels, isDup, rows, cols, origAbsDelta);
			cols+=origAbsDelta;
		}

		//Transpose matrix back if needed
		if (transpose){
			int[][] pixelsResult = new int[cols][rows];
			for (int i = 0 ; i < rows ; i++){
				for(int j = 0 ; j < cols ; j++){
					pixelsResult[j][i] = workPixels[i][j];
				}
			}
			return (pixelsResult);
		}

		return (workPixels);
	}

	private static int[][] energyFunctionReg(int rows, int cols, int jStart, int jEnd, int[][] pixels, int[][] prevEnergyMap)
	{
		if (jStart < 0) jStart = 0;
		if (jEnd > cols) jEnd = cols;

		for( int i = 0; i < rows; i++ )
			for( int j = jStart; j < jEnd; j++ )
				prevEnergyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return prevEnergyMap;
	}

	private static int[][] energyFunctionEnt(int rows, int cols, int jStart, int jEnd, int[][] pixels, int[][] prevEnergyMap)
	{
		if (jStart < 0) jStart = 0;
		if (jEnd > cols) jEnd = cols;

        int entropy;
        int gradient;
        double entWeight = 0.5; //value between 0 to 1. TODO: decide on entropy weight according to experiments
        double gradWeight = 1 - entWeight;
		//int[][] energyMap = new int[rows][cols];

		for( int i = 0; i < rows; i++ ){
            for( int j = jStart; j < jEnd; j++ ){
                gradient = ColorsGradient(i, j, rows, cols, pixels);
                entropy = entropyCalc(i, j, rows, cols, pixels);
				prevEnergyMap[i][j] = (int)(gradient*gradWeight + entropy*entWeight);
            }
        }

		return prevEnergyMap;
	}

	private static int[][] energyFunctionFor(int rows, int cols, int jStart, int jEnd, int[][] pixels, int[][] prevEnergyMap)
	{
		//TODO: IMPLEMENT. right now - same as energyFunctionReg.
		if (jStart < 0) jStart = 0;
		if (jEnd > cols) jEnd = cols;

		for( int i = 0; i < rows; i++ )
			for( int j = jStart; j < jEnd; j++ )
				prevEnergyMap[i][j] = ColorsGradient(i, j, rows, cols, pixels);
		return prevEnergyMap;
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

		Color n;
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

    private static int entropyCalc(int i, int j, int rows, int cols, int[][] pixels)
    {
        //Set iterations bounds
        int mStart = i-4;
        int mEnd = i+4 + 1;
        int nStart= j-4;
        int nEnd = j+4 + 1;

        //Avoid out of range bounds
        if (mStart < 0)  mStart=0;
        if (mEnd > rows) mEnd=rows;
        if (nStart < 0)  nStart=0;
        if (nEnd > cols) nEnd=cols;

        //Help variables
        int acc = 0;
        int denomAcc = 0;
        int Pmn;

        //Calculate Pmn denominator (same for all Pmn)
        for (int m = mStart ; m<mEnd ; m++){
            for (int n = nStart ; n < nEnd ; n++){
                denomAcc += calcFmn(pixels[m][n]);
            }
        }

        //Calculate Hi for pixel (i,j) (up to '-' sign)
        for (int m = mStart ; m<mEnd ; m++){
            for (int n = nStart ; n < nEnd ; n++){
                Pmn = calcFmn(pixels[m][n]) / denomAcc;
                acc += Pmn*Math.log(Pmn);
            }
        }
        return (-acc);
    }

    private static int calcFmn(int pixelValue)
    {
        Color c = new Color(pixelValue);
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();

        return (((c.getRed() + c.getGreen() + c.getBlue()) / 3) + 1);
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

	private static void dynamicProgrammingStep(int[][] energyMap, int[][] dynProgResult, boolean[] lookAt, int i, int j)
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
		if (s == null) return newPixels;

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
		if (s == null) return newPixels;

        for (int row = 0; row < numOfRows; row++)
        {
            int dupIndex = s.cols[row];
            System.arraycopy(pixels[row], 0, newPixels[row], 0, dupIndex+1);

			newPixels[row][dupIndex+1] = duplicateIndex(dupIndex, pixels[row]);

			if (dupIndex != (newNumOfCols-2)){
				System.arraycopy(pixels[row], dupIndex + 1, newPixels[row], dupIndex+2, newNumOfCols - (dupIndex+2));
			}

        }
		return newPixels;
	}

	private static int[][] duplicateCells(int[][] origWorkPixels, boolean[][] isDup, int rows, int cols, int delta){
		int[][] result = new int[rows][cols+delta];
		for (int i = 0 ; i < rows ; i++){
			int currDelta = 0;
			for (int j = 0 ; j < cols ; j++){
				result[i][j+currDelta] = origWorkPixels[i][j];
				if (isDup[i][j]){
					currDelta++;
					result[i][j+currDelta] = duplicateIndex(j, origWorkPixels[i]);
				}
			}
		}
		return result;
	}

	private static int duplicateIndex(int dupIndex, int[] origPixelsRow){
		int cols = origPixelsRow.length;
		if (dupIndex < (cols-1)){
			//Set new pixel as average of its left & right neighbors;
			Color cLeft  = new Color(origPixelsRow[dupIndex]);
			Color cRight = new Color(origPixelsRow[dupIndex+1]);

			int red   = (cLeft.getRed()     + cRight.getRed()   ) / 2;
			int green = (cLeft.getGreen()   + cRight.getGreen() ) / 2;
			int blue  = (cLeft.getBlue()    + cRight.getBlue()  ) / 2;

			int rgb = red;
			rgb = (rgb << 8) + green;
			rgb = (rgb << 8) + blue;

			return rgb;
		}else{
			return origPixelsRow[dupIndex];
		}
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
