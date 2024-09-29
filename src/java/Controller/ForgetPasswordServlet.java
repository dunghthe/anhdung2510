package Controller;

import Dal.AccountDao;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Random;

/**
 *
 * @author ADMIN
 */
public class ForgetPasswordServlet extends HttpServlet {
    
    private final Random random = new Random(); // Class-level Random instance

    // Other methods...

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        AccountDao ad = new AccountDao();
        if (ad.checkAccountByEmail(email) == null) {
            request.setAttribute("messNotExsit", "Email này chưa đăng ký tài khoản");
            request.getRequestDispatcher("forgetPassword.jsp").forward(request, response);
            return;
        }

        // Generate verification code
        String code = generateCode();

        // Send verification code via email
        try {
            sendEmail(email, code);

            // Store the code, email, and creation time in session for later verification
            HttpSession session = request.getSession();
            session.setAttribute("verificationCode", code);
            session.setAttribute("email", email);
            session.setAttribute("codeCreationTime", System.currentTimeMillis());

            // Redirect to the verification page
            response.sendRedirect("verify.jsp");
        } catch (MessagingException e) {
            request.setAttribute("mess", "Gửi email thất bại!");
            request.getRequestDispatcher("forgetPassword.jsp").forward(request, response);
        }
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000); // Use the class-level Random instance
        return String.valueOf(code);
    }

    private void sendEmail(String to, String code) throws MessagingException, UnsupportedEncodingException {
        final String username = "HoLaTechSE1803@gmail.com"; // Your email
        final String password = "xgdm ytoa shxw iwdk"; // Your app password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.mime.charset", "UTF-8"); // Ensure the session uses UTF-8

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        Session session = Session.getInstance(props, auth);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

        // Ensure the subject is set with proper encoding
        message.setSubject(MimeUtility.encodeText("Mã xác nhận ", "UTF-8", "B"));

        // Set the email content with UTF-8 encoding
        message.setContent("Mã xác nhận của bạn là: " + code, "text/plain; charset=UTF-8");

        Transport.send(message);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
