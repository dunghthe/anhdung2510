/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;
import Dal.CartDao;
import Dal.OrderDao;
import Dal.PaymentDao;
import Dal.ProductDao;
import Model.Cart;
import Model.Order;
import Model.OrderDetail;
import Model.ProductVariant;
import Model.User;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
/**
 *
 * @author ADMIN
 */
public class Checkout extends HttpServlet {
    private static final String MESS_PAYMENT = "messpayment";
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet Checkout</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet Checkout at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User acc = (User) session.getAttribute("acc");
        if (acc == null) {
            response.sendRedirect("login.jsp");
        } else {
            CartDao c = new CartDao();
            long total = c.calculateTotalCartPrice(acc.getId());
            List<Cart> list = c.getCartByUid(acc.getId());
            request.setAttribute("listcart", list);
            request.setAttribute("total", total);
            request.setAttribute(MESS_PAYMENT, session.getAttribute(MESS_PAYMENT));
            request.setAttribute("messprofilecheckout", session.getAttribute("messprofilecheckout"));
            request.getRequestDispatcher("checkout.jsp").forward(request, response);
        }
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User acc = (User) session.getAttribute("acc");
        if (acc == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        String paymentMethod = request.getParameter("payment");
        CartDao cartDao = new CartDao();
        ProductDao productDao = new ProductDao();
        List<Cart> cartList = cartDao.getCartByUid(acc.getId());
        long total = cartDao.calculateTotalCartPrice(acc.getId());
        String name = request.getParameter("fullname").trim();
        String phone = request.getParameter("phone").trim();
        String city = request.getParameter("city").trim();
        String district = request.getParameter("district").trim();
        String commune = request.getParameter("commune").trim();
        String address = request.getParameter("address").trim();
        // Validate required fields to ensure they are not empty or whitespace
        if (name.isEmpty() || phone.isEmpty() || city.isEmpty() || district.isEmpty() || commune.isEmpty() || address.isEmpty()) {
            session.setAttribute("messprofilecheckout", "Các trường thông tin không được để trống.");
            session.setAttribute("messprofilecheckoutTime", System.currentTimeMillis()); // Set timestamp
            response.sendRedirect("checkout");
            return;
        }
        // Save data to session
        session.setAttribute("fullname", name);
        session.setAttribute("phone", phone);
        session.setAttribute("city", city);
        session.setAttribute("district", district);
        session.setAttribute("commune", commune);
        session.setAttribute("address", address);
        LocalDateTime currentDate = LocalDateTime.now();
        OrderDao orderDao = new OrderDao();
        if (paymentMethod == null) {
            session.setAttribute(MESS_PAYMENT, "Bạn cần phải chọn 1 trong 2 phương thức thanh toán");
            session.setAttribute("messpaymentTime", System.currentTimeMillis()); // Set timestamp
            response.sendRedirect("checkout");
            return;
        }
        if ("cod".equals(paymentMethod)) {
            // Handle cash on delivery payment
            Order order = new Order();
            order.setUserId(acc);
            order.setName(name);
            order.setPhone(phone);
            order.setProvince(city);
            order.setDistrict(district);
            order.setCommune(commune);
            order.setDetailedAddress(address);
            order.setDate(Date.from(currentDate.atZone(ZoneId.systemDefault()).toInstant()));
            order.setTotal(total);
            order.setStatusid(orderDao.getStatusById(1));
            // Add order details from cart items
            List<OrderDetail> orderDetails = new ArrayList<>();
            for (Cart cart : cartList) {
                OrderDetail detail = new OrderDetail();
                detail.setPid(productDao.getProductById(cart.getPid()));
                detail.setNameProduct(cart.getName());
                ProductVariant variant = productDao.getProductVariantByID(cart.getVariantId());
                if (variant == null) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Product variant not found for cart item.");
                    return;
                }
                detail.setVariantId(variant);
                detail.setPrice(cart.getPrice());
                detail.setQuantity(cart.getQuantity());
                detail.setTotal(cart.getTotalOneProduct());
                orderDetails.add(detail);
            }
            order.setOrderDetails(orderDetails);
            // Perform necessary updates and actions
            for (Cart cart : cartList) {
                productDao.updateProductQuantity(cart.getVariantId(), cart.getQuantity());
            }
            cartDao.clearCart(acc.getId());
            // Add the order to the database
            orderDao.addOrder(order);
            // Get all orders
            int oid = orderDao.findLastOrderId(acc.getId());
            Date date = Date.from(currentDate.atZone(ZoneId.systemDefault()).toInstant());
            PaymentDao paymentDao = new PaymentDao();
            // Convert java.util.Date to java.sql.Date
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            paymentDao.insertPayment(oid, 1, sqlDate, (int) total);
            // Send confirmation email
            try {
                sendEmail(acc.getEmail(), order, cartList);
            } catch (MessagingException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // Redirect to home page or another appropriate page
            response.sendRedirect("home");
        } else if ("vnpay".equals(paymentMethod)) {
            // For VNPAY, you might handle the payment process differently
            request.setAttribute("totalPriceAllProduct", total);
            request.getRequestDispatcher("vnpay_pay.jsp").forward(request, response);
        }
    }
    
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
    
    private void sendEmail(String to, Order order, List<Cart> cartList) throws MessagingException, UnsupportedEncodingException {
        final String username = "HoLaTechSE1803@gmail.com";
        final String password = "xgdm ytoa shxw iwdk";
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.mime.charset", "UTF-8");
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        Session session = Session.getInstance(props, auth);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username, "HoLaTech"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(MimeUtility.encodeText("Order Confirmation", "UTF-8", "B"));
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h1>Order Confirmation</h1>");
        content.append("<p>Thank you for your order. Here are the details:</p>");
        content.append("<table>");
        content.append("<tr><th>Product</th><th>Variant</th><th>Quantity</th><th>Price</th><th>Total</th></tr>");
        for (Cart cart : cartList) {
            ProductVariant variant = cart.getProductVariant();
            if (variant != null) {
                content.append("<tr>")
                    .append("<td>").append(cart.getName()).append("</td>")
                    .append("<td>").append(variant.getName()).append("</td>")
                    .append("<td>").append(cart.getQuantity()).append("</td>")
                    .append("<td>").append(nf.format(cart.getPrice())).append("</td>")
                    .append("<td>").append(nf.format(cart.getTotalOneProduct())).append("</td>")
                    .append("</tr>");
            } else {
                content.append("<tr>")
                    .append("<td>").append(cart.getName()).append("</td>")
                    .append("<td>").append("N/A").append("</td>")
                    .append("<td>").append(cart.getQuantity()).append("</td>")
                    .append("<td>").append(nf.format(cart.getPrice())).append("</td>")
                    .append("<td>").append(nf.format(cart.getTotalOneProduct())).append("</td>")
                    .append("</tr>");
            }
        }
        content.append("</table>");
        content.append("<p>Total: ").append(nf.format(order.getTotal())).append("</p>");
        content.append("</body></html>");
        message.setContent(content.toString(), "text/html; charset=UTF-8");
        Transport.send(message);
    }
    
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
