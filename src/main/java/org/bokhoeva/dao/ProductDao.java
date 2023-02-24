package org.bokhoeva.dao;

import org.bokhoeva.model.Category;
import org.bokhoeva.model.Parameter;
import org.bokhoeva.model.Product;
import org.bokhoeva.model.Unit;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductDao {


    private static Connection connection;

    static {
        String URL = "jdbc:postgresql://localhost:5432/Aston?characterEncoding=utf8";
        String USERNAME = "postgres";
        String PASSWORD = "postgres";


        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }




    public List<Category> getCategories(boolean forProduct, boolean forCategory, int parentId) {
        List<Category> allCategories = new ArrayList<>();
        try {

            Statement statement = connection.createStatement();
            ResultSet resultSet = null;
            if (forProduct) {
                resultSet = statement.executeQuery("select id,name from category where id>1 and parent_id>1");
            }
            if (forCategory) {
                resultSet = statement.executeQuery("select id,name from category");
            }
            if (!forCategory && !forProduct) {
                resultSet = statement.executeQuery("select id, name from category where id>1 and parent_id=" + parentId);
            }

            while (resultSet.next()) {
                Category category = new Category();

                category.setId(resultSet.getInt("id"));
                category.setName(resultSet.getString("name"));

                allCategories.add(category);
            }
        } catch (SQLException e) {
           throw new RuntimeException(e);
        }

        return allCategories;
    }


    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> products = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from product join unit u on product.unit = u.id where category=" + categoryId);

            while (resultSet.next()) {
                Product product = new Product();
                Unit unit = new Unit();

                unit.setNomination(resultSet.getString("nomination"));

                product.setId(resultSet.getInt("id"));
                product.setName(resultSet.getString("name"));
                product.setUnit(unit);
                product.setAmount(resultSet.getBigDecimal("amount").setScale(2, RoundingMode.UNNECESSARY));

                products.add(product);
            }
        } catch (SQLException e) {
           throw new RuntimeException(e);
        }
        return products;
    }

    public Product getProduct(int id) {
        Product product = new Product();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select p.id pid, p.name pname, p.unit punit, p.category pcategory, p.amount pamount, c.name cname, u.nomination unomination from product p join category c on p.category=c.id join unit u on p.unit = u.id where p.id=" + id);
            while (resultSet.next()) {
                String s = resultSet.getCursorName();
                product.setId(resultSet.getInt("pid"));
                product.setName(resultSet.getString("pname"));
                Category category = new Category();
                category.setId(resultSet.getInt("pcategory"));
                category.setName(resultSet.getString("cname"));
                product.setCategory(category);
                product.setAmount(resultSet.getBigDecimal("pamount").setScale(2, RoundingMode.UNNECESSARY));
                Unit unit = new Unit();
                unit.setId(resultSet.getInt("punit"));
                unit.setNomination(resultSet.getString("unomination"));
                product.setUnit(unit);
            }
        } catch (SQLException e) {
           throw new RuntimeException(e);
        }
        return product;
    }

    public void saveProduct(Product product) {
        try {
            Statement statement = connection.createStatement();
            String sql = "insert into product(name,category,unit,amount) values('" + product.getName() + "', " + product.getCategory().getId() + " , " + product.getUnit().getId() + ",'" + product.getAmount() + "')";
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateProduct(Product product) {
        try {

            PreparedStatement statement =
                    connection.prepareStatement("update product set name=?, category=?,unit=?, amount=? where id=?");
            statement.setInt(5, product.getId());
            statement.setString(1, product.getName());
            statement.setInt(2, product.getCategory().getId());
            statement.setInt(3, product.getUnit().getId());
            statement.setBigDecimal(4, product.getAmount().setScale(2, RoundingMode.UNNECESSARY));

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteProduct(int id) {
        try {
            PreparedStatement statement = connection.prepareStatement("delete from product where id=?");
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Category getCategory(int id) {
        Category category = new Category();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from category where id=" + id);

            if (resultSet.next()) {
                Category parentCategory = new Category();


                category.setId(resultSet.getInt("id"));
                category.setName(resultSet.getString("name"));
                int parent_id = resultSet.getInt("parent_id");


                Statement secStatement = connection.createStatement();

                ResultSet secResultSet = secStatement.executeQuery("select name from category where id=" + parent_id);
                if (secResultSet.next()) {
                    parentCategory.setId(parent_id);
                    parentCategory.setName(secResultSet.getString("name"));

                    category.setParent(parentCategory);
                }
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return category;

    }


    public void saveCategory(Category category) {
        try {
            Statement statement = connection.createStatement();
            String sql = "insert into category (name,parent_id) values('" + category.getName() + "','" + category.getParent().getId() + "')";
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void deleteCategory(int id) {
        try {
            PreparedStatement statement = connection.prepareStatement("delete from category where id=?");
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCategory(Category category) {
        try {
            PreparedStatement statement =
                    connection.prepareStatement("update category set name=?, parent_id=? where id=?");
            statement.setInt(3, category.getId());
            statement.setString(1, category.getName());
            statement.setInt(2, category.getParent().getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Parameter> getParametersOfProduct(int productId) {
        List<Parameter> parameters = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet =
                    statement.executeQuery("select * from product_parameter join parameter on product_parameter.parameter_id = parameter.id where product_id=" + productId);

            while (resultSet.next()) {
                Parameter parameter = new Parameter();
                Product product = new Product();
                product.setId(resultSet.getInt("product_id"));
                parameter.setProduct(product);
                parameter.setName(resultSet.getString("name"));
                parameter.setValue(resultSet.getString("value"));

                parameters.add(parameter);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return parameters;


    }

    public List<Unit> getAllUnits() {
        List<Unit> units = new ArrayList<>();
        try {

            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("select * from unit");

            while (resultSet.next()) {
                Unit unit = new Unit();

                unit.setId(resultSet.getInt("id"));
                unit.setNomination(resultSet.getString("nomination"));

                units.add(unit);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return units;
    }


}
