/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.filechooser.*;

public class FilenameCryptor extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8563950575121859973L;
	JButton pathButton, encryptButton, decryptButton;
	JTextArea log;
	JFileChooser dirChooser;

	String path;

	public FilenameCryptor() {
		super(new BorderLayout());

		// Create a file chooser
		dirChooser = new JFileChooser();

		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		pathButton = new JButton("Select Path...");
		pathButton.addActionListener(this);

		encryptButton = new JButton("Encrypt");
		encryptButton.addActionListener(this);

		decryptButton = new JButton("Decrypt");
		decryptButton.addActionListener(this);

		// For layout purposes, put the buttons in a separate panel
		JPanel buttonPanel = new JPanel(); // use FlowLayout
		buttonPanel.add(encryptButton);
		buttonPanel.add(decryptButton);

		// Add the buttons and the log to this panel.
		add(pathButton, BorderLayout.PAGE_START);
		add(buttonPanel, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {

		String password;
		if (e.getSource() == pathButton) {
			dirChooser.showOpenDialog(FilenameCryptor.this);
			path = dirChooser.getSelectedFile().getAbsolutePath();

		} else if (e.getSource() == encryptButton) {
			if (path == null || path.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Choose a directory first");
			} else {
				password = requestPassword(true);
				encrypt(password, path);
			}

			return;

		} else if (e.getSource() == decryptButton) {
			if (path == null || path.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Choose a directory first");
			} else {
				password = requestPassword(false);
				decrypt(password, path);
			}
			return;
		}
	}

	private void copyFile(String path, String oldname, String newname) {
		// File (or directory) with old name
		Path src = Paths.get(path + "/" + oldname);

		// File (or directory) with new name
		Path dst = Paths.get(path + "/" + newname);

		if (new File(path + "/" + newname).exists()) {
			System.out.println("file exists");
			return;
		}

		// Rename file (or directory)
		try {
			Files.copy(src, dst);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void encrypt(String password, String path) {
		List<String> files = listFiles(path);
		Cipher cipher = initCrypto(password, Cipher.ENCRYPT_MODE);

		File outputDir = new File(path + "/encrypted");
		if (outputDir.exists()) {
			JOptionPane.showMessageDialog(null, "Directory " + path
					+ "/encrypted already exists");
		} else {
			outputDir.mkdir();
		}

		for (String currentfile : files) {

			String name;
			String extension;

			int i = currentfile.lastIndexOf('.');
			if (i > 0) {
				name = currentfile.substring(0, i);
				extension = currentfile.substring(i + 1);
			} else {
				name = currentfile;
				extension = "";

			}

			byte[] encrypted = { 0x0 };
			try {
				encrypted = cipher.doFinal(name.getBytes());
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String newName = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(encrypted);
			newName += "." + extension;
			copyFile(path, currentfile, "/encrypted/" + newName);

		}
	}

	private Cipher initCrypto(String password, int mode) {

		Cipher cipher = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] passwordHash = digest.digest(password
					.getBytes(StandardCharsets.UTF_8));

			SecretKeySpec skeyspec = new SecretKeySpec(passwordHash, "AES");
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(mode, skeyspec, new IvParameterSpec(new byte[16]));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return cipher;
	}

	private void decrypt(String password, String path) {
		List<String> files = listFiles(path);
		Cipher cipher = initCrypto(password, Cipher.DECRYPT_MODE);

		File outputDir = new File(path + "/decrypted");
		if (outputDir.exists()) {
			JOptionPane.showMessageDialog(null, "Directory " + path
					+ "/decrypted already exists");
		} else {
			outputDir.mkdir();
		}

		for (String currentfile : files) {

			String name;
			String extension;

			int i = currentfile.lastIndexOf('.');
			if (i > 0) {
				name = currentfile.substring(0, i);
				extension = currentfile.substring(i + 1);
			} else {
				name = currentfile;
				extension = "";

			}

			byte[] decrypted = { 0x0 };
			try {

				decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(name));
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

			String newName = new String(decrypted);
			newName += "." + extension;
			copyFile(path, currentfile, "/decrypted/" + newName);

		}
	}

	private List<String> listFiles(String path) {
		File directory = new File(path);
		File[] listOfFiles = directory.listFiles();
		List<String> list = new ArrayList<String>();

		for (File currentFile : listOfFiles) {
			if (currentFile.isFile()) {
				list.add(currentFile.getName());
			}
		}

		return list;
	}

	private String requestPassword(boolean doubleCheck) {
		JPanel panel = new JPanel(new GridLayout(2, 2, 2, 2));
		JFrame frame = new JFrame();

		JPasswordField passwordField = new JPasswordField();
		panel.add(new JLabel("Password", SwingConstants.RIGHT));
		panel.add(passwordField);

		JPasswordField repetitionField = new JPasswordField();
		if (doubleCheck) {
			panel.add(new JLabel("Repeat Password", SwingConstants.RIGHT));
			repetitionField = new JPasswordField();
			panel.add(repetitionField);
		}

		JOptionPane.showMessageDialog(frame, panel, "Enter password",
				JOptionPane.QUESTION_MESSAGE);

		if (doubleCheck)
			while (!Arrays.equals(passwordField.getPassword(),
					repetitionField.getPassword())) {

				JOptionPane.showMessageDialog(frame, panel,
						"Passwords did not match!",
						JOptionPane.QUESTION_MESSAGE);

			}

		return new String(passwordField.getPassword());
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private static void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("Filename Cryptor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new FilenameCryptor());

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowGUI();
			}
		});
	}
}
