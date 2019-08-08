/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import fit5192.zz.repository.UserRepository;
import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.User_;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.NamedQuery;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;

/**
 *
 * @author Zheng Ru
 */
@Stateless
public class JPAUserRepositoryImpl implements UserRepository {
    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;

    public JPAUserRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }
    
    @Override
    public void addUser(User_ user) throws  Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(user);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchUserById(user.getId()) != null) {
                throw new PreexistingEntityException("User " + user + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
     @Override
    public void removeUserById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            User_ user;
            try {
                user = entityManager.getReference(User_.class, id);
                user.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The user with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(user);
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
    public void updateUser(User_ user) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            user = entityManager.merge(user);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = user.getId();
                if (searchUserById(id) == null) {
                    throw new NonexistentEntityException("The user with id " + id + " no longer exists.");
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
    public User_ searchUserById(int id) throws Exception {    
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(User_.class, id);
        } finally {
            entityManager.close();
        }
    }
    //for register
    public List<User_> searchUserByEmail(String email){
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        Query query = entityManager.createNamedQuery("User_.searchUserByEmail");
        System.out.println(query.toString());
        query.setParameter("email", email);
        return query.getResultList();
    }
    @Override
    public List<User_> getAllUsers() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("User_.findAll").getResultList();
    }

/*
    public List<User> findUserEntities(int maxResults, int firstResult) {
        return findUserEntities(false, maxResults, firstResult);
    }

    private List<User> findUserEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(User_.class));
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

    public int getUserCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<User> rt = cq.from(User_.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
*/
}
