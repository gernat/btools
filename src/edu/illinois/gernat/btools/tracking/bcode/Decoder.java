/*
 * Copyright (C) 2017 University of Illinois Board of Trustees.
 *
 * This file is part of bTools.
 *
 * bTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * bTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bTools. If not, see http://www.gnu.org/licenses/.
 */

package edu.illinois.gernat.btools.tracking.bcode;

import java.util.Arrays;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public final class Decoder
{
	
	public static final int FIELD_ID = 0;
	
	public static final int FIELD_ERROR_CORRECTION_FLAG = 1;
		
	public static int[] decode(BitMatrix matrix) throws ReedSolomonException
	{
		int[] bytes = Decoder.parseMatrix(matrix);
		int[] copy = Arrays.copyOf(bytes, bytes.length);		
		bytes = Decoder.correctErrors(bytes);
		int id = ((bytes[0] << BCode.BLOCK_SIZE) & 0xFF00) + (bytes[1] & 0xFF);
		if (id >= BCode.UNIQUE_ID_COUNT) throw new ReedSolomonException("Invalid number");
		int errorCorrectionFlag = Arrays.equals(bytes, copy) ? 0 : 1;
		int[] result = {id, errorCorrectionFlag};
		return result;
	}

	private static int[] parseMatrix(BitMatrix matrix)
	{
		int count = 0;
		int[] bytes = new int[BCode.DATA_BYTES + BCode.ERROR_CORRECTION_BYTES];
		for (int i = 0; i < bytes.length; i++)
		{
			
			// skip masked bits
			int start = 0;
			if ((i == 0) && (BCode.MASKED_BIT_COUNT > 0)) start = BCode.MASKED_BIT_COUNT;
			
			// decode bytes
			for (int j = start; j < BCode.BLOCK_SIZE; j++)
			{
				bytes[i] = bytes[i] << 1;
				boolean bit = matrix.get(BCode.DATA_XY[count][0], BCode.DATA_XY[count][1]);
				if (bit) bytes[i] += 1;
				count++;
			}
			
		}
		return bytes;
	}
	
	private static int[] correctErrors(int[] bytes) throws ReedSolomonException
	{
		ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
		decoder.decode(bytes, BCode.ERROR_CORRECTION_BYTES);
		return bytes;
	}

}
