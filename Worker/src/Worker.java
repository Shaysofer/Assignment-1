import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRScanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.asprise.util.ocr.OCR;

public class Worker {
	private PropertiesCredentials Pc;
	private AWSCredentials Credentials;
	private AmazonS3 S3;
	private AmazonSQS AmazonSQS;
	private int numOfJobs;

	public Worker() {
		try {
			Pc = new PropertiesCredentials(
					Worker.class
							.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Credentials = Pc;
		S3 = new AmazonS3Client(Credentials);
		AmazonSQS = new AmazonSQSClient(Pc);
		numOfJobs = -1;

	}

	public void setNumOfJobs() {
		if (numOfJobs == -1) {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
					ConstantProvider.MANAGER_TO_WORKER_QUEUE);
			List<Message> messages = AmazonSQS.receiveMessage(
					receiveMessageRequest).getMessages();
			numOfJobs = Integer.parseInt(messages.get(0).getBody());
			String messageRecieptHandle = messages.get(0).getReceiptHandle();
			AmazonSQS.deleteMessage(new DeleteMessageRequest(
					ConstantProvider.MANAGER_TO_WORKER_QUEUE,
					messageRecieptHandle));

		}
	}

	private String getUrlToWorkOn() {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				ConstantProvider.MESSEAGES_QUEUE);
		receiveMessageRequest.setVisibilityTimeout(0);
		receiveMessageRequest.setMaxNumberOfMessages(1);
		String ans = "";
		try {
			Message currentMessege = AmazonSQS
					.receiveMessage(receiveMessageRequest).getMessages().get(0);
			ans = currentMessege.getBody();
			deleteMessegeFromQueue("Deleting messege " + currentMessege
					+ "from messeges queue", ConstantProvider.MESSEAGES_QUEUE);
		} catch (Exception e) {
			System.out.println("No more Messeges in queue");
			ans = null;
		}
		return ans;
	}

	private void sendMessege(String messege, String to) {
		System.out.println("Send messege " + messege + " to " + to);
		AmazonSQS.sendMessage(new SendMessageRequest(to, messege));

	}

	private void deleteMessegeFromQueue(String messegeToPerform, String Queue) {
		System.out.println(messegeToPerform + ".\n");
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				Queue);
		String messageRecieptHandle = AmazonSQS
				.receiveMessage(receiveMessageRequest).getMessages().get(0)
				.getReceiptHandle();
		AmazonSQS.deleteMessage(new DeleteMessageRequest(Queue,
				messageRecieptHandle));

	}

	public static void main(String args[]) throws Exception {
		Worker worker = new Worker();
		// worker.setNumOfJobs();
		String urlToWork = worker.getUrlToWorkOn();
		int messeageDid = 0;

		while (urlToWork != null) {
			URL url = new URL(urlToWork);
			BufferedImage image = ImageIO.read(url);
			String encode = new OCR().recognizeEverything(image);
			worker.sendMessege(urlToWork + "1qazxsw2@WSXZAQ!" + encode,
					ConstantProvider.ENCODED_IMAGE);
			urlToWork = worker.getUrlToWorkOn();
			messeageDid++;

		}
		worker.sendMessege("Worker done and did" + messeageDid + " messeages",
				ConstantProvider.WORKER_TO_MANAGER_QUEUE);

	}

}
