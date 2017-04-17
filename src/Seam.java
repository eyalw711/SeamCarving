
public class Seam 
{
	public int[] cols;
	public int rows;
	public int min;
	public int max;
	
	Seam(int rows)
	{
		this.rows = rows;
		this.cols = new int[rows];
		this.min = Integer.MAX_VALUE;
		this.max = -1;
	}
	
	Seam(int[] cols, int rows)
	{
		this.cols = cols;
		this.rows = rows;
		this.min = Integer.MAX_VALUE;
		this.max = -1;
	}

	public void form(int[][] dynProgResult, int mode) 
	{
		int rows = dynProgResult.length;
		int cols = dynProgResult[0].length;
		
		boolean[] lookAt = new boolean[3];
		if (mode == SeamCarving.VERTICAL)
		{
			lookAt[0] = false;
			lookAt[1] = true;
			lookAt[2] = false;
		}
		else //SeamCarving.SEAM
		{
			lookAt[0] = lookAt[1] = lookAt[2] = true;
		}
		
		//look at lowest row to find correct seam:
		int minimalIndexFound = lowestRowMinimalValueIndex(dynProgResult, rows, cols);
		this.min = minimalIndexFound;
		this.max = minimalIndexFound;

		//int currRow = rows - 1;
		int currCol = minimalIndexFound;
		
		for (int i = rows - 1; i > -1; i--)
		{
			//put at seam
			this.cols[i] = currCol;
			if (currCol < this.min) this.min = currCol;
			if (currCol > this.max) this.max = currCol;
			
			//find next col:
			if(i == 0)
				break;
			
			if ((currCol == 0 || currCol == cols -1) && (mode == SeamCarving.VERTICAL_SEAM)) //edge case
			{
				if (currCol == 0)
				{
					lookAt[0] = false;
					//step
					minimalIndexFound = formSeamUpperCol(dynProgResult, lookAt, currCol, i);
					lookAt[0] = true;
				}
				else
				{
					lookAt[2] = false;
					//step
					minimalIndexFound = formSeamUpperCol(dynProgResult, lookAt, currCol, i);
					lookAt[2] = true;
				}
			}
			else
			{
				//step
				minimalIndexFound = formSeamUpperCol(dynProgResult, lookAt, currCol, i);
			}
			
			currCol = minimalIndexFound;
		}
	}

	private int formSeamUpperCol(int[][] dynProgResult, boolean[] lookAt, int currCol, int i) {
		int minimalIndexFound = -1;
		int minimalValueFound = Integer.MAX_VALUE;
		
		for (int k = 0; k < 3; k++)
		{
			if (lookAt[k])
			{
				if (dynProgResult[i-1][currCol + k - 1] < minimalValueFound)
				{
					minimalValueFound = dynProgResult[i - 1][currCol + k - 1];
					minimalIndexFound = currCol + k - 1;
				}
			}
		}
		return minimalIndexFound;
	}

	private int lowestRowMinimalValueIndex(int[][] dynProgResult, int rows, int cols) {
		int minimalValueFound = Integer.MAX_VALUE;
		int minimalIndexFound = -1;
		for (int j = 0; j < cols; j++)
		{
			if (dynProgResult[rows - 1][j] < minimalValueFound)
			{
				minimalValueFound = dynProgResult[rows - 1][j];
				minimalIndexFound = j;
			}
		}
		return minimalIndexFound;
	}
	
}




