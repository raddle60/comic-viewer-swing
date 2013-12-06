package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ComicViewer {

	private JFrame frame;
	private JPanel picPane;
	private JScrollPane scrollPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final ComicViewer window = new ComicViewer();
					window.frame.setExtendedState(Frame.MAXIMIZED_VERT);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ComicViewer() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 651, 467);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu menu = new JMenu("文件");
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem("打开");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawImage();
			}
		});
		menu.add(menuItem);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		scrollPane = new JScrollPane();
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

		picPane = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
				Image image = getToolkit().getImage("D:/017_23889.png");
				int x = 0;
				if (this.getWidth() > image.getWidth(null)) {
					x = (int) ((this.getWidth() - image.getWidth(null)) / 2.0);
				}
				g.drawImage(image, x, 0, null);
				System.out.println("paint");
			}

		};
		scrollPane.setViewportView(picPane);
		picPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
		// /
	}

	private void drawImage() {
		Image image = picPane.getToolkit().getImage("D:/017_23889.png");
		picPane.setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
	}
}
