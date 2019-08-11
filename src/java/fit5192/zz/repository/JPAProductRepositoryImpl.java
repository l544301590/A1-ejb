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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

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
    public void addProduct(Product product) throws  Exception {
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
        StringBuilder query=new StringBuilder("SELECT p FROM product p WHERE 1");
        if(product.getId()!=0){
            query.append("AND p.id=:id");
        }
        if(isEmpty(product.getName())){
            query.append("AND p.name=:name");
        }
        if(product.getCategory()!=0){
            query.append("AND p.category=:category");
        }
        if(isEmpty(product.getArea())){
            query.append("AND p.area=:area");
        }
        if(product.getPrice()!=0){
            query.append("AND p.price<:price");
        }
        String orderQuery = "SELECT AVG(s.value) FROM Rating r WHERE r.product=p DESC";//这里的r.product=p 感觉不太对啊
        String finalQuery = query.toString()+"ORDER BY"+orderQuery;
        TypedQuery<Product> q = entityManager.createQuery(finalQuery, Product.class);         
        q.setParameter("id", product.getId());
        q.setParameter("name", product.getName());
        q.setParameter("category", product.getCategory());
        q.setParameter("area", product.getArea());
        q.setParameter("price", product.getPrice());
        return q.getResultList();  
    }
    public static boolean isEmpty(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0||str==" ") {
        return true;
    }
    for (int i = 0; i < strLen; i++) {
        if ((Character.isWhitespace(str.charAt(i)) == false)) {
            return false;
        }
    }
    return true;
    }
    
    
   
    public List<Product> searchProductByAnyAttribute1(Product product) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        int id = product.getId();
        String name = product.getName();
        String imgPath = product.getImgPath();
        int category = product.getCategory();
        String area = product.getArea();
        float price = product.getPrice();
        int inventory = product.getInventory();//存货
        String description = product.getDescription();
        HashMap<String,Object> constraint=new HashMap<>();
        if(id!=0){
            constraint.put("id",id );
        }
        if(!isEmpty(name)){
            constraint.put("name",name );
        }
        if(!isEmpty(imgPath)){
            constraint.put("imgPath",imgPath );
        }
        if(category!=0){
            constraint.put("category",category );
        }
        if(!isEmpty(area)){
            constraint.put("area",area );
        }
        if(price!=0){
            constraint.put("price",price );
        }
        if(inventory!=-1){
            constraint.put("inventory",price );
        }
        if(!isEmpty(description)){
        //Fuzzy queries may be required. 
            constraint.put("description",description );
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = criteriaBuilder.createQuery(Product.class);
        Root<Product> products = query.from(Product.class);
        Subquery<Double> subquery = query.subquery(Double.class);//<>define the returns  entity’s type
        Root<Rating> ratings = subquery.from(Rating.class);
        //Join<Project, Employee> sqEmp = project.join(Project_.employees); 为了得到rating.pid=pid 是否需要join
        subquery.select(criteriaBuilder.avg(ratings.get(Rating_.value)))
               // .where(criteriaBuilder.equal(ratings.get(Rating_.product),criteriaBuilder.parameter(Product.class,"Product") ));//为什么自动转成了Double类型
                .where(criteriaBuilder.equal(ratings.get("product"),products))
        List<Predicate> predicatesList = new ArrayList<>();
        
        for(Object key : constraint.keySet()) {
            String attr = (String)key;
            Object value = constraint.get(attr);
            predicatesList.add(criteriaBuilder.equal(resultProducts .get(attr), value));
        }
        query.where(predicatesList.toArray(new Predicate[predicatesList.size()]));
        TypedQuery<Product> q = entityManager.createQuery(query);
        List<Product> disorderProductList=q.getResultList();
        return disorderProductList;  
        //后面可以用   product.getRating 得到对应的rating 然后计算平均分
        //用HashMap存储，最后对value排序
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



}
