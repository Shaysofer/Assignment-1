import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.asprise.util.ocr.OCR;

class Worker {

	public static void main(String args[]) throws Exception{
		OCR.setLibraryPath("/home/ec2-user/libAspriseOCR.so");
		AmazonSQS sqs = new AmazonSQSClient(new PropertiesCredentials(
				Worker.class.getResourceAsStream("AwsCredentials.properties")));
		String url  =ConstantProvider.MESSEAGES_QUEUE;
		ReceiveMessageRequest recieve = new ReceiveMessageRequest(url);
		List<Message> messages = sqs.receiveMessage(recieve).getMessages();
		while (messages.size() != 0){
			String mas = messages.get(0).getBody();
			URL url1 = new URL(mas);
			BufferedImage image = ImageIO.read(url1);
			String s = new OCR().recognizeEverything(image);
			sqs.sendMessage(new SendMessageRequest(ConstantProvider.WORKER_TO_MANAGER_QUEUE,s));
			String messageRecieptHandle = messages.get(0).getReceiptHandle();
			sqs.deleteMessage(new DeleteMessageRequest(url, messageRecieptHandle));
			messages = sqs.receiveMessage(recieve).getMessages();
			System.out.println("a");
		}
		sqs.sendMessage(new SendMessageRequest(ConstantProvider.WorketToManagerFinish,"finish"));
		System.out.println("messege sent!!!");
		
	}
}
