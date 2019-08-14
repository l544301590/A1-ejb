/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import fit5192.zz.repository.ProductRepository;
import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.Product;
import fit5192.zz.repository.entities.Rating;
import fit5192.zz.repository.entities.Rating_;
import fit5192.zz.repository.entities.Transaction_;
import fit5192.zz.repository.entities.User_;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author Zheng Ru
 */
@Stateless
public class JPAProductRepositoryImpl implements ProductRepository {

    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;

    public JPAProductRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }

    @Override
    public void addProduct(Product product) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(product);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchProductById(product.getId()) != null) {
                throw new PreexistingEntityException("Product " + product + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Override
    public void removeProductById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Product product;
            try {
                product = entityManager.getReference(Product.class, id);
                product.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The product with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(product);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Override
    public void updateProduct(Product product) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            product = entityManager.merge(product);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = product.getId();
                if (searchProductById(id) == null) {
                    throw new NonexistentEntityException("The product with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Override
    public Product searchProductById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(Product.class, id);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public List<Product> getAllProducts() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("Product.findAll").getResultList();
    }

    @Override
    public List<Product> searchProductByAnyAttribute(Product product) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        StringBuilder query = new StringBuilder("SELECT p FROM Product p WHERE 1=1 ");
        if (product.getId() != 0) {
            query.append(" AND p.id=:id");
        }
        if (!isEmpty(product.getName())) {
            query.append(" AND p.name=:name");
        }
        if (product.getCategory() != 0) {
            query.append(" AND p.category=:category");
        }
        if (!isEmpty(product.getArea())) {
            query.append(" AND p.area=:area");
        }
        if (product.getPrice() != 0) {
            query.append(" AND p.price<=:price");
        }
        String orderQuery = "SELECT AVG(r.value) FROM Rating r WHERE r.product.id=p.id";//这里的r.product=p 感觉不太对啊
        String finalQuery = query.toString() + " ORDER BY " + "( " + orderQuery + " )";
        //System.err.println("最终的查询语句"+finalQuery);
        TypedQuery<Product> q = entityManager.createQuery(finalQuery, Product.class);
        if (product.getId() != 0) {
            q.setParameter("id", product.getId());
        }
        if (!isEmpty(product.getName())) {
            q.setParameter("name", product.getName());
        }
        if (product.getCategory() != 0) {
            q.setParameter("category", product.getCategory());
        }
        if (!isEmpty(product.getArea())) {
            q.setParameter("area", product.getArea());
        }
        if (product.getPrice() != 0) {
            q.setParameter("price", product.getPrice());
        }
//        System.out.println(q.toString());
//        System.err.println(q.getResultList().toString());
        return q.getResultList();
    }

    public static boolean isEmpty(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0 || str == " ") {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Product> getAllProductsSorted(User_ user) {
        EntityManager entityManager1 = this.entityManagerFactory.createEntityManager();
        
        // tend to purchase expensive or not
        int avgPrice = 0;
        int numBought = 0;
        
        // get all products the user has bought (duplicates may exist)
        List<Product> products = new ArrayList<>();
        Query query1 = entityManager1.createNamedQuery("Transaction_.SearchTransactionsByUserId");
        query1.setParameter("userId", user.getId());
        List<Transaction_> transactions = query1.getResultList();
        for (Transaction_ transaction : transactions) {
            for (Product p : transaction.getProducts()) {
                products.add(p);
                avgPrice += p.getPrice();
                numBought += 1;
            }
        }
        avgPrice /= numBought;
        
        // map<category, quantity>
        Map<Integer, Integer> map = new HashMap<>();
        for (Product product : products) {
            if (map.containsKey(product.getCategory())) {
                map.put(product.getCategory(), map.get(product.getCategory()) + 1);
            } else {
                map.put(product.getCategory(), 1);
            }
        }
        
        // sort by map key (keen on which category)
        List<Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<Integer, Integer>>() {
            @Override
            public int compare(Entry<Integer, Integer> o1,
                    Entry<Integer, Integer> o2) {
                int flag = o2.getValue().compareTo(o1.getValue());
                if (flag == 0) {
                    return o2.getKey().compareTo(o1.getKey());
                }
                return flag;
            }
        });
        
        EntityManager entityManager2 = this.entityManagerFactory.createEntityManager();
        List<Product> res = new ArrayList<>();
        // Assume only 4 categories
        for (Entry<Integer, Integer> e : list) {
            int category = e.getKey();
            String queryString = "SELECT p FROM Product p WHERE p.category=:category ORDER BY (SELECT AVG(r.value) FROM Rating r WHERE r.product.id=p.id), p.price";
            if (avgPrice > 10) {
                queryString += " DESC";
            }
            Query query2 = entityManager2.createQuery(queryString);
            query2.setParameter("category", category);
            res.addAll(query2.getResultList());
        }
        
        return res;
    }
    /*
    public List<Product> findProductEntities(int maxResults, int firstResult) {
        return findProductEntities(false, maxResults, firstResult);
    }

    private List<Product> findProductEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Product.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public int getProductCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Product> rt = cq.from(Product.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
     */
 /*
    @Override
    public List<Product> searchTest() {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        TypedQuery<Product> p =  entityManager.createQuery("SELECT p FROM Product p WHERE 1=1 AND p.category=1 ORDER BY (SELECT AVG(r.value) FROM Rating r WHERE r.product.id=p.id )", Product.class);         
//        q.setParameter("firstname", firstname);    
//        q.setParameter("surname", surname);
        List<Product> resultList = p.getResultList();
        System.err.println("有输出了吗？！！"+resultList.toString());
        return p.getResultList();
    }
     */

}
