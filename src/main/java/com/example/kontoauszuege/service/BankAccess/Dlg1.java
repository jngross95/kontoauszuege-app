package com.example.kontoauszuege.service.BankAccess;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class Dlg1 extends JDialog  {

    public String tan = null;

    public Dlg1(String kontaktName, String inputFieldText, String message, byte[] image) throws Exception {
        super(new JFrame(), "BankingSrv: "+kontaktName, true);
        
        // Prüfen ob Grafikumgebung verfügbar ist
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Grafische Umgebung nicht verfügbar (Headless Mode)");
        }

        //Message
        JPanel messagePane = new JPanel();
        var jTextArea = new JTextArea(message);
        jTextArea.setLineWrap(true);
        jTextArea.setSize(1200,100);
        messagePane.add(jTextArea);
        getContentPane().add(messagePane, BorderLayout.NORTH);

        if(image != null) {
            ByteArrayInputStream stream = new ByteArrayInputStream(image);


            JPanel picPane = new JPanel();
            //BufferedImage myPicture = ImageIO.read(new File("d:\\img.png"));
            BufferedImage myPicture = ImageIO.read(stream);
            JLabel picLabel = new JLabel(new ImageIcon(myPicture));
            picPane.add(picLabel);

            getContentPane().add(picPane, BorderLayout.CENTER);
        }

        //getContentPane().add(picLabel, BorderLayout.CENTER);

        //Ok
        JPanel buttonPane = new JPanel();
        var l = new JLabel(inputFieldText);
        var t = new JTextField(20);
        JButton button = new JButton("OK");
        buttonPane.add(l);
        buttonPane.add(t);
        buttonPane.add(button);

        button.addActionListener(e -> {
            setVisible(false);
            dispose();
        });

        getContentPane().add(buttonPane, BorderLayout.SOUTH);


        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
        tan = t.getText();
    }

}