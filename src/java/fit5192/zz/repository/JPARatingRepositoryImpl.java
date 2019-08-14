/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import fit5192.zz.repository.RatingRepository;
import fit5192.zz.repository.entities.Product;
import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.Rating;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;

/**
 *
 * @author Zheng Ru
 */
@Stateless
public class JPARatingRepositoryImpl implements RatingRepository {

    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;

    public JPARatingRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }
    @Override
    public void addRating(Rating rating) throws  Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(rating);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchRatingById(rating.getId()) != null) {
                throw new PreexistingEntityException("Rating " + rating + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
     @Override
    public void removeRatingById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Rating rating;
            try {
                rating = entityManager.getReference(Rating.class, id);
                rating.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The Rating with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(rating);
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
    public void updateRating(Rating rating) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            rating = entityManager.merge(rating);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = rating.getId();
                if (searchRatingById(id) == null) {
                    throw new NonexistentEntityException("The Rating with id " + id + " no longer exists.");
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
    public Rating searchRatingById(int id) throws Exception {      
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(Rating.class, id);
        } finally {
            entityManager.close();
        }
    }
    
     @Override
    public List<Rating> getAllRating() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("Rating.findAll").getResultList();
    }
    
    @Override
    public List<Rating> searchRatingsByProductId(int productId) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        Query query = entityManager.createNamedQuery("Rating.searchRatingByProductId");
        query.setParameter("productId", productId);
        return query.getResultList();
    }
    /*
    @Override
    public Map<Product,Float> getAvgValueOfProduct(Product product) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        
        //Assume that the input product is deterministic, so the product found according to the corresponding property should be unique
        List<Product> productAttribute = productRepository.searchProductByAnyAttribute(product); 
        Query query = entityManager.createNamedQuery("searchRatingByProduct");
        query.setParameter("product",productAttribute.get(0) );         
        List<Rating> ratings = query.getResultList();
        float valueSum = 0;
        for(Rating r:ratings){
            valueSum+=r.getValue();
        }
        Float average = valueSum/ratings.size();
        Map<Product,Float> valueAverage= new HashMap<Product,Float>();
        valueAverage.put(productAttribute.get(0),  average );
        return valueAverage;
    }
    
    public List<Rating> searchRatings() {
        return findRatingEntities(true, -1, -1);
    }

    public List<Rating> findRatingEntities() {
        return findRatingEntities(true, -1, -1);
    }

    public List<Rating> findRatingEntities(int maxResults, int firstResult) {
        return findRatingEntities(false, maxResults, firstResult);
    }

    private List<Rating> findRatingEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Rating.class));
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


    public int getRatingCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Rating> rt = cq.from(Rating.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
   */   

}
