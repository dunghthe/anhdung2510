package Dal;

import Model.Category;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for Category.
 * Handles database interactions for Category related operations.
 */
public class CategoryDao extends DBContext {

    // Logger để ghi log thông tin, cảnh báo và lỗi
    private static final Logger logger = Logger.getLogger(CategoryDao.class.getName());

    /**
     * Truy xuất tất cả danh mục từ cơ sở dữ liệu.
     * @return Danh sách các đối tượng Category.
     */
    public List<Category> getAllCategory() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM Category";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Category category = new Category();
                category.setId(rs.getInt(1));
                category.setName(rs.getString(2));
                list.add(category);
            }
            logger.info("Fetched all categories successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while fetching all categories", e);
        }

        return list;
    }

    /**
     * Truy xuất danh mục theo ID từ cơ sở dữ liệu.
     * @param id ID của danh mục.
     * @return Đối tượng Category hoặc null nếu không tìm thấy.
     */
    public Category getCategoryById(int id) {
        Category category = null;
        String sql = "SELECT * FROM Category WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    category = new Category();
                    category.setId(rs.getInt("id"));
                    category.setName(rs.getString("name"));
                }
            }
            logger.info("Fetched category with ID: " + id);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while fetching category by ID", e);
        }

        return category;
    }

    /**
     * Thêm một danh mục mới vào cơ sở dữ liệu.
     * @param name Tên của danh mục mới.
     */
    public void insertCategory(String name) {
        String query = "INSERT INTO [dbo].[Category] ([name]) VALUES (?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);  
            ps.executeUpdate();
            // Thay System.out.println bằng Logger
            logger.info("Category inserted successfully with name: " + name);
        } catch (SQLException e) {
            // Thay System.out.println bằng Logger để log lỗi
            logger.log(Level.SEVERE, "SQL Error while inserting category: {0}", e.getMessage());
        }
    }

    /**
     * Cập nhật tên của một danh mục hiện có.
     * @param cid ID của danh mục cần cập nhật.
     * @param name Tên mới của danh mục.
     */
    public void updateCategory(int cid, String name) {
        String sql = "UPDATE [dbo].[Category] SET [name] = ? WHERE id = ?";

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, name);
            st.setInt(2, cid);
            st.executeUpdate();
            logger.info("Updated category ID " + cid + " with new name: " + name);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while updating category", e);
        }
    }

    /**
     * Xóa một danh mục sau khi kiểm tra xem có sản phẩm nào liên kết với danh mục đó không.
     * @param cid ID của danh mục cần xóa.
     * @return Thông báo về kết quả xóa.
     */
    public String deleteCategory(int cid) {
        String checkSql = "SELECT COUNT(*) FROM [dbo].[Product] WHERE cid = ?";
        String deleteSql = "DELETE FROM [dbo].[Category] WHERE id = ?";
        String message = "";

        try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
            checkSt.setInt(1, cid);
            try (ResultSet rs = checkSt.executeQuery()) {
                if (rs.next()) {
                    int productCount = rs.getInt(1);
                    if (productCount > 0) {
                        message = "Không thể xóa danh mục này. Có " + productCount +
                                  " sản phẩm được liên kết với danh mục này. Vui lòng xóa hoặc chỉ định lại các sản phẩm trước khi xóa danh mục.";
                        return message;
                    }
                }
            }

            // Tiến hành xóa danh mục nếu không có sản phẩm liên kết
            try (PreparedStatement deleteSt = connection.prepareStatement(deleteSql)) {
                deleteSt.setInt(1, cid);
                deleteSt.executeUpdate();
                message = "Xóa danh mục thành công.";
                logger.info("Deleted category ID: " + cid);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while deleting category", e);
            message = "SQL Error: " + e.getMessage();
        }

        return message;
    }

    /**
     * Lấy số lượng danh mục cùng với số lượng sản phẩm trong mỗi danh mục.
     * @return Danh sách các đối tượng Category với số lượng sản phẩm.
     * @throws Exception nếu có lỗi xảy ra trong cơ sở dữ liệu.
     */
    public List<Category> getCategoryCounts() throws Exception {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.id, c.name, COUNT(p.id) AS count " +
                     "FROM Category c LEFT JOIN Product p ON c.id = p.cid " +
                     "GROUP BY c.id, c.name";

        try (PreparedStatement st = connection.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int count = rs.getInt("count");
                list.add(new Category(id, name, count));
            }
            logger.info("Fetched category counts successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving category counts", e);
            throw new Exception("Error retrieving category counts", e);
        }

        return list;
    }

    /**
     * Lấy tổng số lượng danh mục hiện có trong cơ sở dữ liệu.
     * @return Tổng số danh mục.
     */
    public int getTotalCategory() {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM Category";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
            logger.info("Fetched total category count: " + count);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while getting total categories", e);
        }

        return count;
    }

    /**
     * Phương thức main để kiểm tra và thử nghiệm các phương thức.
     */
    public static void main(String[] args) {
        CategoryDao cd = new CategoryDao();
        int total = cd.getTotalCategory();
        logger.info("Total categories: " + total);
    }
}
