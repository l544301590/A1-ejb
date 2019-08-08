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
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

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
