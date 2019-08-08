/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.Transaction_;
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
public class JPATransactionRepositoryImpl implements TransactionRepository {

    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;

    public JPATransactionRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }
    @Override
    public void addTransaction(Transaction_ tran) throws  Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(tran);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchTransactionById(tran.getId()) != null) {
                throw new PreexistingEntityException("Transaction " + tran + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
     @Override
    public void removeTransactionById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Transaction_ tran;
            try {
                tran = entityManager.getReference(Transaction_.class, id);
                tran.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The transaction with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(tran);
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
    public void updateTransaction(Transaction_ tran) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            tran = entityManager.merge(tran);//the error maybe exsist
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = tran.getId();
                if (searchTransactionById(id) == null) {
                    throw new NonexistentEntityException("The Transaction with id " + id + " no longer exists.");
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
    public Transaction_ searchTransactionById(int id) throws Exception {  
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(Transaction_.class, id);
        } finally {
            entityManager.close();
        }
    }
    
    @Override
    public List<Transaction_> getAllTransactions() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("Transaction.findAll").getResultList();
    }

/*
    public List<Transaction> findTransactionEntities(int maxResults, int firstResult) {
        return findTransactionEntities(false, maxResults, firstResult);
    }

    private List<Transaction> findTransactionEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Transaction_.class));
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

    public int getTransactionCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Transaction> rt = cq.from(Transaction_.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    */
}
