package com.library.gui.admin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AdminDashboard extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(1100, 700); // Slightly larger window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 240, 240)); // Lighter background

        // Sidebar Panel
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(280, getHeight())); // Wider sidebar
        sidebar.setBackground(new Color(50, 65, 90)); // Dark blue-gray
        sidebar.setLayout(new GridLayout(10, 1, 5, 10)); // Tighter spacing
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10)); // Add padding

        // Content Panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(255, 255, 255)); // White content area
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding

        // Adding Panels
        contentPanel.add(new BookManagementPanel(), "Manage Books");
        contentPanel.add(new UserManagementPanel(), "Manage Users");
        contentPanel.add(new TransactionsPanel(), "View Transactions");
        contentPanel.add(new OverdueBooksPanel(), "View Overdue Books");
        contentPanel.add(new ReportsPanel(), "Reports");
        contentPanel.add(new FineManagementPanel(), "Fine Management");
        contentPanel.add(new AdminBorrowBookPanel(), "Borrow Book");
        contentPanel.add(new AdminReturnBookPanel(), "Return Book");

        // Sidebar Buttons
        String[] buttons = {
                "Manage Books",
                "Manage Users",
                "View Transactions",
                "View Overdue Books",
                "Reports",
                "Fine Management",
                "Borrow Book",
                "Return Book",
                "Logout"
        };

        for (String text : buttons) {
            JButton button = new JButton(text);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // More modern font
            button.setBackground(new Color(70, 90, 120)); // Darker blue
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(250, 50)); // Taller buttons
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Add hover effect
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(90, 120, 160)); // Lighter on hover
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(70, 90, 120)); // Original color
                }
            });

            // Make logout button stand out
            if (text.equals("Logout")) {
                button.setBackground(new Color(180, 70, 70)); // Red color for logout
                button.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        button.setBackground(new Color(200, 90, 90)); // Lighter red on hover
                    }
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        button.setBackground(new Color(180, 70, 70)); // Original red
                    }
                });
            }

            sidebar.add(button);

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (text.equals("Logout")) {
                        dispose(); // Close the window on logout
                    } else {
                        cardLayout.show(contentPanel, text);
                    }
                }
            });
        }

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminDashboard dashboard = new AdminDashboard();
            dashboard.setVisible(true);
        });
    }
}