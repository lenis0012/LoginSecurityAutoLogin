package net.minecraft.src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import net.minecraft.client.Minecraft;

public class mod_LoginSecurityAutoLogin extends BaseMod {
	private final char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_".toCharArray();
	private final Random random = new Random();
	private Minecraft mc;
	private String server;
	private String password;
	
	@Override
	public String getName() {
		return "LoginSecurity";
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public void load() {
		this.mc = ModLoader.getMinecraftInstance();
		this.getDataFolder().mkdirs();
		ModLoader.setInGameHook(this, true, true);
		ModLoader.registerPacketChannel(this, "LoginSecurity");
		System.out.println(this.getName() + " v" + this.getVersion() + " Succesfully loaded!");
	}
	
	@Override
	public void clientConnect(final NetClientHandler handler) {
		final INetworkManager networkManager = handler.getNetManager();
		if(networkManager != null) {
			this.server = networkManager.getSocketAddress().toString();
		} else
			this.server = null;
		
		this.password = null;
		this.sendCustomPayload("Q_PING");
	}
	
	@Override
	public void clientCustomPayload(NetClientHandler handler, Packet250CustomPayload packet) {
		String channel = packet.channel;
		System.out.println(channel);
		if(channel.equals("LoginSecurity")) {
			try {
				String message = new String(packet.data, "UTF-8");
				System.out.println(message);
				if(message.startsWith("Q_")) {
					this.onQuestionReceive(message.substring(2));
				} else if(message.startsWith("A_")) {
					String[] data = message.substring(2).split(" ", 2);
					String question = data[0];
					String answer = data[1];
					this.onAnswerReceive(question, answer);
				}
			} catch (UnsupportedEncodingException e) {
				System.err.println("Failed to decode message");
				e.printStackTrace();
			}
		}
	}
	
	public void onQuestionReceive(String question) {
		if(question.equals("LOGIN")) {
			this.sendCustomPayload("Q_REG");
		}
	}
	
	public void onAnswerReceive(String question, String answer) {
		if(question.equals("REG")) {
			boolean registered = Boolean.parseBoolean(answer);
			if(registered) {
				this.loadPassword();
				if(this.password != null) {
					this.sendCustomPayload("A_LOGIN " + password);
				}
			} else
				this.sendCustomPayload("Q_REQ");
		} else if(question.equalsIgnoreCase("REQ")) {
			boolean required = Boolean.parseBoolean(answer);
			if(required) {
				this.generateRandomPassoword();
				this.sendCustomPayload("A_REG " + password);
				this.savePassowrd();
			}
		} else if(question.equals("PASS")) {
			if(!password.equals(answer)) {
				this.password = answer;
				this.savePassowrd();
			}
		}
	}
	
	public void loadPassword() {
		if(this.server == null)
			return;
		
		File file = new File(this.getDataFolder(), server + ".dat");
		if(!file.exists())
			return;
		
		BufferedReader reader = null;
		try {
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			reader = new BufferedReader(isr);
			String line = reader.readLine();
			if(line != null)
				this.password = line;
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					;
				}
			}
		}
	}
	
	public void savePassowrd() {
		if(this.server == null || this.password == null)
			return;
		
		File file = new File(this.getDataFolder(), server + ".dat");
		file.getParentFile().mkdirs();
		System.out.println(file.getAbsolutePath());
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		BufferedWriter writer = null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			writer = new BufferedWriter(osw);
			writer.write(password + '\n');
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					;
				}
			}
		}
	}
	
	public void generateRandomPassoword() {
		StringBuilder builder = new StringBuilder(10);
		for(int i = 0; i < 10; i++) {
			builder.append(chars[random.nextInt(chars.length)]);
		}
		
		this.password = builder.toString();
	}
	
	public void sendCustomPayload(String message) {
		Packet250CustomPayload packet = new Packet250CustomPayload();
		packet.channel = "LoginSecurity";
		packet.data = message.getBytes();
		packet.length = packet.data.length;
		ModLoader.clientSendPacket(packet);
	}
	
	public File getDataFolder() {
		return new File(Minecraft.getMinecraftDir(), "mods" + File.separator + this.getName());
	}
}
