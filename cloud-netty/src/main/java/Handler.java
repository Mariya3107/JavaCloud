import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class Handler extends ChannelInboundHandlerAdapter {

    public enum State {
        EMPTY, NAME_LENGTH, NAME, FILE_LENGTH, FILE, OPERATION_SIZE, OPERATION  ///состояния Handler (пустой, длина имени, имя, длина файла, файл)
    }

    private State currentState = State.EMPTY;
    private int nextLength;
    private int operationSize;
    private long fileLength;
    private long gettedFileLength;
    private BufferedOutputStream outputStream;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);  //получает ByteBuffer
        while (buf.readableBytes() > 0) {   //пытается вычитать все из буфера
            if (currentState == State.EMPTY) {
                byte read = buf.readByte(); //если ничего в данный момент не происходит, то читает первый байт
                if (read == (byte) 10) {  //если первый байт = 10, то переходит в состояние ожидания имени файла
                    currentState = State.NAME_LENGTH; //следующим ожидает длину имени
                    gettedFileLength = 0L;
                    System.out.println("State: Initial component receiving");
                } else {
                    System.out.println("ERROR: Invalid first byte: " + read);
                }
            }
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 15) {  //сигнальный байт о получении имени файла
                    System.out.println("STATE: Getting length of Filename");
                    nextLength = buf.readInt();  //вычитывает длину имени из буфера
                    currentState = State.NAME;  //ожидает имя файла
                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength]; //создает байтовый массив
                    buf.readBytes(fileName); //печатает имя файла
                    System.out.println("STATE: Filename: " + new String(fileName) + " getting");
                    outputStream = new BufferedOutputStream(new FileOutputStream("copy_ " + new String(fileName))); //открывает поток для записи файла
                    currentState = State.FILE_LENGTH;  //следующим ожидает длину файла
                }
            }
            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 20) { //сигнальный байт о длине (объеме) файла
                    fileLength = buf.readLong(); //вычитывает длину файла
                    System.out.println("STATE: Getting Filelength" + fileLength);
                    currentState = State.FILE; //следующим ожидает сам файл
                }
            }
            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {  //до тех пор, пока буфер не пустой
                    outputStream.write(buf.readByte()); //поток вычитывает все из буфера, заливает в файл
                    gettedFileLength++; //читает с первого байта до конца, прибавляя каждый раз по 1
                    if (fileLength == gettedFileLength) { //когда ожидаемая длина файла равна количеству полученных байт
                        currentState = State.EMPTY; //переходит в состояние "Нчего не происходит"
                        System.out.println("Process finished");
                        outputStream.close(); //поток закрывается
                        break; //процесс передачи одного файла останавливается
                    }
                }
            }
            if (currentState == State.EMPTY) {
                byte read = buf.readByte();
                if (read == (byte) 4) {
                    currentState = State.OPERATION_SIZE;
                    System.out.println("State: Initial component receiving");
                } else {
                    System.out.println("ERROR: Invalid first byte: " + read);
                }
            }
            if (currentState == State.OPERATION_SIZE) {
                if (buf.readableBytes() >= 5) {
                    operationSize = buf.readInt();
                    if (operationSize == 0){
                        ctx.writeAndFlush("NullOperation".getBytes("UTF-8"));
                        currentState = State.EMPTY;
                    } else{
                        operationSize = 5;
                        currentState = State.OPERATION;
                    }
                }
            }
            if (currentState == State.OPERATION) {
                if (buf.readableBytes() >= operationSize) {
                    ctx.fireChannelRead(buf.readBytes(operationSize));
                    currentState = State.EMPTY;
                }
            } break;

        }
        if(buf.readableBytes() == 0){  //если буфер пустой, сбрасывает его
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }

}


