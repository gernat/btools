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

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public final class Encoder
{

	private Encoder()
	{
	}

	public static BitMatrix encode(int data)
	{
		if ((data < 0) || (data > Math.pow(2, BCode.DATA_BYTES * BCode.BLOCK_SIZE - BCode.MASKED_BIT_COUNT) - 1)) throw new IllegalArgumentException();		
		BitMatrix code = BCode.createTemplate(); 
		int[] bytes = Encoder.appendErrorCorrectionBytes(data);
		Encoder.writeDataBits(code, bytes);
		return code;
	}
	
	private static int[] appendErrorCorrectionBytes(int data)
	{
		int[] bytes = new int[BCode.DATA_BYTES + BCode.ERROR_CORRECTION_BYTES];
		bytes[0] = (data >> BCode.BLOCK_SIZE) & 0xFF;
		bytes[1] = data & 0xFF;
		ReedSolomonEncoder encoder = new ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256);
		encoder.encode(bytes, BCode.ERROR_CORRECTION_BYTES);		
		return bytes;
	}

	private static BitMatrix writeDataBits(BitMatrix code, int[] data)
	{
		int count = 0;
		for (int i = 0; i < data.length; i++)
		{
			
			// skip masked bits
			int start = 0;
			if ((i == 0) && (BCode.MASKED_BIT_COUNT > 0)) 
			{
				start = BCode.MASKED_BIT_COUNT;
				data[i] = data[i] << BCode.MASKED_BIT_COUNT;
			}

			// encode bits
			for (int j = start; j < BCode.BLOCK_SIZE; j++)
			{
				boolean bit = (data[i] & 0x80) > 0;
				if (bit) code.set(BCode.DATA_XY[count][0], BCode.DATA_XY[count][1]);
				data[i] = data[i] << 1;
				count++;
			}
			
		}
		return code;
	}

}
