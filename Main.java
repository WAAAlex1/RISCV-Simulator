import java.io.*;
import java.util.*;

public class Main
{
    static int PC;
    static int[] regArray = new int[32];
    static boolean running;
    static byte[] memory = new byte[0x100000];
    static String testFileName;

    public static void main(String[] args) throws java.io.IOException
    {
        Scanner scanner = new Scanner(System.in);
        readBinFile(scanner);

        //Control variables
        
        running = true; //control variable for breaking while loop
        PC = 0;  //Program Counter - initialized to 0

        int instruction;

        //program:
        while(running)
        {
            System.out.println("---------------------- INSTRUCTION AT PC = 0x" + Integer.toHexString(PC) + " STARTING --------------------------");

            //Instantiate Values
            instruction = (0x000000ff & memory[PC]) | (0x0000ff00 & (memory[PC+1] << 8)) | (0x00ff0000 & (memory[PC+2]) << 16) | (0xff000000 & (memory[PC+3] << 24));

            //ASSERT INVARIABLE VALUES
            regArray[0] = 0;

            //run instruction
            executeInstruction(instruction);
            printOutput();

            //Update Values for next instruction
            PC += 4; //+4 bytes because 4 bytes pr instruction
        }
        //output
        binaryDump();
        System.out.println(compareResult() ? "PROGRAM SUCCESS":"PROGRAM FAILED");
    }

    //  FUNCTIONS FOR FILE-HANDLING AND OUTPUT ----------------------------------------------------------------

    public static void readBinFile(Scanner scanner) throws java.io.IOException
    {
        File instructionFile = pickInstructionFile(scanner, "Enter instruction-file name:");
        FileInputStream fin = new FileInputStream(instructionFile);
        DataInputStream dataIn = new DataInputStream(fin);

        System.out.println("Loading instructions:");

        int numBytes = (int) instructionFile.length();

        for (int i = 0; i < numBytes; i++)
        {
            memory[i] = dataIn.readByte();
        }
    }

    public static File pickInstructionFile(Scanner scanner, String message)
    {
        System.out.print(message);
        if(scanner.hasNext())
        {
            String fileName = scanner.next();
            testFileName = fileName;
            File input = new File(fileName + ".bin");
            scanner.nextLine();
            if(input.exists())
            {
                System.out.println("File \"" + input + "\" selected.");
                return input;
            }
        }
        else
        {
            scanner.nextLine();
        }
        return pickInstructionFile(scanner, message);
    }

    public static void printOutput()
    {
        System.out.println("---------------------- REGISTER VALUES AFTER INSTRUCTION --------------------------");
        int x = 0;
        for(int i = 0; i < 8 ; i++)
        {
            for(int j = x; j <= x+3 ; j++)
            {
                String str = Integer.toHexString(regArray[j]);
                while(str.length() < 8) str = "0" + str;
                System.out.print("x"+j+" = 0x" + str + " " + "\t");
            }
            x += 4;
            System.out.println();
        }
    }

    public static void binaryDump() throws java.io.IOException
    {
        FileOutputStream binFile = new FileOutputStream( "our" + testFileName + ".res");
        DataOutputStream dataOut = new DataOutputStream(binFile);
        for (int i = 0; i < regArray.length; i++)
        {
            dataOut.writeByte(regArray[i]);
            dataOut.writeByte(regArray[i] >>> 8);
            dataOut.writeByte(regArray[i] >>> 16);
            dataOut.writeByte(regArray[i] >>> 24);
        }
        binFile.close();
    }

    public static boolean compareResult() throws java.io.IOException
    {
        File file1 = new File("our" + testFileName + ".res");
        File file2 = new File(testFileName + ".res");
        byte[] result = new byte[(int) file1.length()];
        byte[] expected = new byte[(int) file2.length()];
        FileInputStream fis1 = new FileInputStream(file1);
        FileInputStream fis2 = new FileInputStream(file2);
        fis1.read(result);
        fis2.read(expected);

        boolean equal = true;

        for(int i = 0; i < file1.length(); i++)
        {
            if(result[i] != expected[i]) equal = false;
        }
        return equal;
    }

    //  FUNCTIONS FOR SIMUlATING -----------------------------------------------------------------------------

    //Function which executes the given function using a switch statement
    public static void executeInstruction(int instr)
    {
        int opcode = (instr << 25) >>> 25; //get 7 LSB as opcode
        System.out.println("Opcode = " + opcode);
        switch(opcode){
            case 51:    //R-type (basic instructions)
                execute51InstrType(instr);
                break;
            case 19:    //I-type (immediates)
                execute19InstrType(instr);
                break;
            case 3:     //I-type (load)
                execute3InstrType(instr);
                break;
            case 35:    //S-type (store)
                execute35InstrType(instr);
                break;
            case 99:    //B-type (branch)
                execute99InstrType(instr);
                break;
            case 111:    //J-type (Jump and link)
                execute111InstrType(instr);
                break;
            case 103:    //I-type (Jump and link register)
                execute103InstrType(instr);
                break;
            case 55:    //U-type (Load Upper Imm)
                execute55InstrType(instr);
                break;
            case 23:    //U-type (Add Upper Imm to PC)
                execute23InstrType(instr);
                break;
            case 115:    //I-type (ecall)
                execute115InstrType(instr);
                break;
            default:
                System.out.println("Unexpected opcode at PC = " + Integer.toHexString(PC));
                break;
        }
    }

    public static void execute51InstrType(int instr) //basics
    {
        byte funct7 = (byte) (instr >>> 25);
        byte rs2 = (byte) ((instr >>> 20) & 0x1f);
        byte rs1 = (byte) ((instr >>> 15) & 0x1f);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);
        byte rd = (byte) ((instr >>> 7) & 0x1f);

        //execute basic instructions here...
        switch(funct3)
        {
            case 0:
                if(funct7 == 0b0)
                {
                    regArray[rd] = regArray[rs1] + regArray[rs2];
                } else {
                    regArray[rd] = regArray[rs1] - regArray[rs2];
                }
                break;
            case 1:
                regArray[rd] = regArray[rs1] << regArray[rs2];
                break;
            case 2:
                regArray[rd] = (regArray[rs1] < regArray[rs2]) ? 1 : 0;
                break;
            case 3:
                regArray[rd] = ((regArray[rs1] & 0xFF) < (regArray[rs2] & 0xFF)) ? 1 : 0;
                break;
            case 4:
                regArray[rd] = regArray[rs1] ^ regArray[rs2];
                break;
            case 5:
                if(funct7 == 0b0)
                {
                    regArray[rd] = regArray[rs1] >>> regArray[rs2];
                } else {
                    regArray[rd] = regArray[rs1] >> regArray[rs2];
                }
                break;
            case 6:
                regArray[rd] = regArray[rs1] | regArray[rs2];
                break;
            case 7:
                regArray[rd] = regArray[rs1] & regArray[rs2];
                break;
            default:
                break;
        }
    }

    public static void execute19InstrType(int instr) //immediate
    {
        int imm = instr >> 20;
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        //execute immediate instructions here...
        switch(funct3)
        {
            case 0:
                regArray[rd] = regArray[rs1] + imm;
                break;
            case 1:
                if((imm & 0xFE0) == 0b0) regArray[rd] = regArray[rs1] << (imm & 0x1F);
                break;
            case 2:
                regArray[rd] = (regArray[rs1] < imm) ? 1 : 0;
                break;
            case 3:
                regArray[rd] = ((regArray[rs1] & 0xFF) < (imm & 0xFFF)) ? 1 : 0;
                break;
            case 4:
                regArray[rd] = regArray[rs1] ^ imm;
                break;
            case 5: // srli/srai
                System.out.println("Shift right immediate = " + Integer.toBinaryString(imm));
                if((imm & 0xFE0) == 0x0) //srli
                {
                    regArray[rd] = regArray[rs1] >>> (imm & 0x1F);
                }
                else if ((imm & 0xFE0) >>> 5 == 0x20) //srai
                {
                    regArray[rd] = regArray[rs1] >> (imm & 0x1F);
                }
                break;
            case 6:
                regArray[rd] = regArray[rs1] | imm;
                break;
            case 7:
                regArray[rd] = regArray[rs1] & imm;
                break;
            default:
                break;
        }
    }

    public static void execute3InstrType(int instr) //load
    {
        int imm = instr >>> 20;
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        //scuffed: signed bit extension. If bit 12 is 1 then set all bits further left to 1.
        if(imm >>> 11 == 1) imm |= 0xfffff000;

        //Introduce helpful variable to increase readability
        int address = regArray[rs1] + imm;

        // load value from memory (byte), sign extend to (int), then store in rd. Address found by adding rs1 to sign extended imm
        switch(funct3)
        {
            case 0: //load byte (signed)
                regArray[rd] = memory[address];
                break;
            case 1: //load halfword (signed)
                // load bytes into their right slots in rd by using bitshifting. The bytes in lower positions must be extracted as unsigned.
                regArray[rd] = ((0x000000ff & memory[address]) | (memory[address + 1] << 8));
                break;
            case 2: //load word
                // same as above, here we simply load more bytes. No byte should be sign extended. Use & to avoid this.
                regArray[rd] = (0x000000ff & memory[address] | (0x0000ff00 & (memory[address + 1] << 8)) | (0x00ff0000 & (memory[address + 2] << 16)) | (0xff000000 & (memory[address + 3] << 24)) );

                System.out.println("Loaded word: " + Integer.toHexString(memory[address + 3]) + " " + Integer.toHexString(memory[address + 2]) + " " + Integer.toHexString(memory[address + 1]) + " " + Integer.toHexString(memory[address]));
                
                break;
            case 4: //load byte unsigned
                //load byte as unsigned using built in function for bytes.
                regArray[rd] = Byte.toUnsignedInt(memory[address]);
                break;
            case 5: //load halfword unsigned
                //load bytes into their right slots in rd by bitshifting by 8 while adding 1 to our memory address. Loaded as unsigned.
                regArray[rd] = Byte.toUnsignedInt(memory[address]) | (Byte.toUnsignedInt(memory[address + 1]) << 8);
                break;
            default:
                System.out.println("Unexpected load intruction");
                break;
        }
    }

    public static void execute35InstrType(int instr) //store
    {
        int imm = ((instr >>> 20) & 0xfe0) | ((instr >>> 7) & 0x1F);
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte rs2 = (byte) ((instr >>> 20) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);

        //scuffed: signed bit extension. If bit 12 is 1 then set all bits further left to 1.
        if(imm >>> 11 == 1) imm |= 0xfffff000;

        //execute store instructions here...
        //Use static memory array here

        int address = regArray[rs1] + imm;

        switch(funct3)
        {
            case 0: //store byte
                memory[address] = (byte) (0x000000ff & regArray[rs2]);
                break;
            case 1: //store halfword
                memory[address] = (byte) (0x000000ff & regArray[rs2]);
                memory[address + 1] = (byte) ((0x0000ff00 & regArray[rs2]) >>> 8);
                break;
            case 2: //store word
                //scuffed
                System.out.println("sw trying to access address with rs1= " + rs1 + " which contains: " + regArray[rs1] + " and imm: " + imm);
                System.out.println("In sw RS2 is register: " + rs2 + " and the content is: 0x" + Integer.toHexString(regArray[rs2]));
                System.out.println("Saved word: " + Integer.toHexString(memory[address + 3]) + " " + Integer.toHexString(memory[address + 2]) + " " + Integer.toHexString(memory[address + 1]) + " " + Integer.toHexString(memory[address]));

                memory[address] = (byte) (0x000000ff & regArray[rs2]);
                memory[address + 1] = (byte) ((0x0000ff00 & regArray[rs2]) >>> 8);
                memory[address + 2] = (byte) ((0x00ff0000 & regArray[rs2]) >>> 16);
                memory[address + 3] = (byte) ((0xff000000 & regArray[rs2]) >>> 24);

                break;
            default:
                System.out.println("Unexpected store instruction");
                break;
        }
    }

    public static void execute99InstrType(int instr) //branch
    {
        int imm = ((instr << 4) & 0x800) | ((instr >>> 7) & 0x1E) | ((instr >>> 20) & 0x7E0) | ((instr >>> 19) & 0x1000);
        byte rs2 = (byte) ((instr >>> 20) & 0x1F);
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);

        //scuffed: signed bit extension. If bit 13 is 1 then set all bits further left to 1.
        if(imm >>> 12 == 1) imm |= 0xfffff000;

        System.out.println("Branch immediate =" + Integer.toBinaryString(imm));

        switch(funct3)
        {
            case 0: //beq
                if(regArray[rs1] == regArray[rs2]) PC += imm - 4; //-4 because we always add 4 after each instr
                break;
            case 1: //bne
                if(regArray[rs1] != regArray[rs2]) PC += imm - 4;
                break;
            case 4: //blt
                if(regArray[rs1] < regArray[rs2]) PC += imm - 4;
                break;
            case 5:
                if(regArray[rs1] >= regArray[rs2]) PC += imm - 4;
                break;
            case 6: //bltu
                if(Integer.compareUnsigned(regArray[(rs1)],regArray[(rs2)]) < 0) PC += imm - 4;
                break;
            case 7: //bgeu
                if(Integer.compareUnsigned(regArray[(rs1)],regArray[(rs2)]) >= 0) PC += imm - 4;
                break;
            default:
                System.out.println("Unexpected branch instruction");
                break;
        }
    }

    public static void execute111InstrType(int instr) //jal
    {
        //imm[20|10:1|11|19:12] <- disgusting, but should work.
        int imm = ((instr >>> 11) & 0x100000) | ((instr >>> 20) & 0x7FE) | ((instr >>> 9) & 0x800) | (instr & 0xFF000);
        //int imm = ((instr >>> 11) & 0x100000) | ((instr >>> 11) & 0x7FE) | ((instr >>> 11) & 0x800) | ((instr >>> 11) & 0xFF000);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        System.out.println("JAL immediate: " + Integer.toBinaryString(imm));

        //scuffed: signed bit extension. If bit 21 is 1 then set all bits further left to 1.
        if(imm >>> 20 == 1) imm |= 0xfff00000;

        //execute jump and link instruction here...
        regArray[rd] = PC + 4;
        PC += imm - 4;
    }

    public static void execute103InstrType(int instr) //jalr
    {
        int imm = instr >>> 20;
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        System.out.println("JALR immediate: " + Integer.toBinaryString(imm));

        //execute JALR instruction here...
        if(funct3 == 0)
        {
            regArray[rd] = PC + 4;
            PC = regArray[rs1] + imm - 4;
        }
        else
        {
            System.out.println("Unexpected jalr funct3 value");
        }

    }

    public static void execute55InstrType(int instr) //lui
    {
        int imm = (instr & 0xfffff000);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        //execute load upper immediate instruction here:
        regArray[rd] = imm;
    }

    public static void execute23InstrType(int instr) //auipc
    {
        int imm = (instr & 0xfffff000);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        System.out.println("AUIPC immediate: " + Integer.toHexString(imm));

        //execute add/load upper immediate to PC instruction here
        regArray[rd] = PC + imm;
    }

    public static void execute115InstrType(int instr) //ecall
    {
        int imm = instr >>> 20;
        byte rs1 = (byte) ((instr >>> 15) & 0x1F);
        byte funct3 = (byte) ((instr >>> 12) & 0x7);
        byte rd = (byte) ((instr >>> 7) & 0x1F);

        //execute enviroment call instruction here
        //see the git for all ecalls we neeed to implement
        switch(regArray[17]) //a7
        {
            case 1: //print_int
                System.out.println(regArray[10]); //prints the content of a0 as an int
                break;
            case 2: //print_float
                System.out.println( (float) regArray[10] ); //this prolly isnt what is meant
                break;
            case 4: //print_string
                while(true)
                {
                    if(memory[regArray[10]] == 0)
                    {
                        System.out.print("\n");
                        break;
                    }

                    System.out.print((char) regArray[10]);
                }
                break;
            case 10: //exit
                running = false;
                break;
            case 11: //print_char
                System.out.println((char) regArray[10]);
                break;
            case 34: //print_hex
                System.out.println(Integer.toHexString(regArray[10]));
                break;
            case 35: //print_bin
                System.out.println(Integer.toBinaryString(regArray[10]));
                break;
            case 36: //print_unsigned
                System.out.println(Integer.toUnsignedLong(regArray[10]));
                break;
            case 93: //exit (with status code in a0)
                System.out.println("Exit status: " + regArray[10]);
                running = false;
                break;
            default:
                System.out.println("Unexpected ecall");
                break;
        }
    }
}
