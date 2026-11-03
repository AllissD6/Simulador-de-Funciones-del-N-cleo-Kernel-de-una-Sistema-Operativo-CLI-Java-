package simulador_so;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Random;

// --- ESTRUCTURAS DE DATOS ---
class PCB {
    String pid;
    String estado;
    int tamano;
    int rafagaTotal;
    int rafagaRestante;

    public PCB(String pid, int tamano, int rafagaTotal) {
        this.pid = pid;
        this.estado = "NUEVO";
        this.tamano = tamano;
        this.rafagaTotal = rafagaTotal;
        this.rafagaRestante = rafagaTotal;
    }
}

class Memoria {
    int capacidadTotal = 100; // 100 MB[cite: 3]
    int memoriaDisponible = 100;

    public boolean asignar(int tamano) {
        if (tamano <= memoriaDisponible) {
            memoriaDisponible -= tamano;
            return true;
        }
        return false;
    }

    public void liberar(int tamano) {
        memoriaDisponible += tamano;
    }
}

class SemaforoBinario {
    int valor = 1;
    Queue<PCB> colaEspera = new LinkedList<>();

    public boolean wait(PCB proceso) {
        if (valor == 1) {
            valor = 0;
            return true;
        } else {
            colaEspera.add(proceso);
            proceso.estado = "BLOQUEADO_SEMAFORO";
            return false;
        }
    }

    public PCB signal() {
        if (!colaEspera.isEmpty()) {
            PCB procesoLiberado = colaEspera.poll();
            procesoLiberado.estado = "LISTO";
            return procesoLiberado;
        } else {
            valor = 1;
            return null;
        }
    }
}

class GestorDeadlock {
    boolean recurso1Libre = true;
    boolean recurso2Libre = true;
    PCB procesoA = null;
    PCB procesoB = null;
    boolean enDeadlock = false;

    public void simularDeadlock(PCB pA, PCB pB) {
        this.procesoA = pA;
        this.procesoB = pB;
        this.recurso1Libre = false;
        this.recurso2Libre = false;
        this.enDeadlock = true;
        System.out.println("\n¡ALERTA DE DEADLOCK DETECTADO![cite: 3]");
        System.out.println("Proceso " + pA.pid + " retiene Recurso 1 y solicita Recurso 2.");
        System.out.println("Proceso " + pB.pid + " retiene Recurso 2 y solicita Recurso 1.");
    }

    public void abortarProceso(String opcion, PlanificadorRR planificador) {
        if (!enDeadlock) {
            System.out.println("No hay deadlock activo.");
            return;
        }

        PCB procesoAbortado = null;
        PCB procesoSalvado = null;

        if (opcion.equals("A")) {
            procesoAbortado = procesoA;
            procesoSalvado = procesoB;
            recurso1Libre = true;
        } else if (opcion.equals("B")) {
            procesoAbortado = procesoB;
            procesoSalvado = procesoA;
            recurso2Libre = true;
        } else {
            System.out.println("Opción invalida.");
            return;
        }

        System.out.println("\nAbortando forzosamente el Proceso " + procesoAbortado.pid + "...[cite: 3]");
        planificador.memoria.liberar(procesoAbortado.tamano);
        procesoAbortado.estado = "TERMINADO (ABORTO)";
        
        System.out.println("Ciclo de interbloqueo roto.[cite: 3]");
        enDeadlock = false;
        
        procesoSalvado.estado = "LISTO";
        planificador.colaListos.add(procesoSalvado);
        System.out.println("Proceso " + procesoSalvado.pid + " devuelto a la cola de listos.");
        
        procesoA = null;
        procesoB = null;
    }
}

class PlanificadorRR {
    Queue<PCB> colaListos = new LinkedList<>();
    int quantum = 3; // Quantum fijo q=3[cite: 3]
    Memoria memoria = new Memoria();
    SemaforoBinario semaforo = new SemaforoBinario();
    GestorDeadlock gestorDeadlock = new GestorDeadlock();
    int pidCounter = 1;
    Random rand = new Random();

    public void crearProceso(int tamano, int rafaga) {
        if (memoria.asignar(tamano)) {
            PCB nuevoProceso = new PCB("P" + (pidCounter++), tamano, rafaga);
            nuevoProceso.estado = "LISTO";
            colaListos.add(nuevoProceso);
            System.out.println("Proceso " + nuevoProceso.pid + " creado exitosamente. Memoria restante: " + memoria.memoriaDisponible + "MB");
        } else {
            System.out.println("\nError: Out of Memory. Proceso rechazado por falta de espacio RAM. Se requieren " + tamano + "MB, disponibles " + memoria.memoriaDisponible + "MB.[cite: 3]");
        }
    }

    public void ejecutarSiguiente() {
        if (gestorDeadlock.enDeadlock) {
            System.out.println("\nEl sistema está bloqueado por un Deadlock. Resuelva el conflicto primero.");
            return;
        }

        if (colaListos.isEmpty()) {
            System.out.println("No hay procesos en la cola de listos.");
            return;
        }

        PCB procesoActual = colaListos.poll();
        procesoActual.estado = "EJECUTANDO";
        
        int tiempoEjecucion = Math.min(quantum, procesoActual.rafagaRestante);
        System.out.println("\n--- Ejecutando Proceso " + procesoActual.pid + " ---");
        System.out.println("Rafaga restante inicial: " + procesoActual.rafagaRestante);
        
        if (rand.nextDouble() > 0.7) { 
             System.out.println(procesoActual.pid + " intenta entrar a la Sección Critica...");
             if (semaforo.wait(procesoActual)) {
                 System.out.println(procesoActual.pid + " entró a la Sección Critica. Modificando variable...");
                 semaforo.signal(); 
                 System.out.println(procesoActual.pid + " salio de la Seccion Critica.");
             } else {
                 System.out.println(procesoActual.pid + " bloqueado por Semaforo. Enviado a cola de espera.[cite: 3]");
                 return; 
             }
        }

        procesoActual.rafagaRestante -= tiempoEjecucion;
        System.out.println("Ejecutado por " + tiempoEjecucion + " unidades. Rafaga restante: " + procesoActual.rafagaRestante);

        if (procesoActual.rafagaRestante <= 0) {
            procesoActual.estado = "TERMINADO";
            memoria.liberar(procesoActual.tamano);
            System.out.println("Proceso " + procesoActual.pid + " ha TERMINADO. Memoria disponible: " + memoria.memoriaDisponible + "MB");
        } else {
            procesoActual.estado = "LISTO";
            colaListos.add(procesoActual);
            System.out.println("Proceso " + procesoActual.pid + " sufre cambio de contexto. Regresa al final de la cola.[cite: 3]");
        }

        PCB procesoDesbloqueado = semaforo.signal();
        if (procesoDesbloqueado != null) {
             colaListos.add(procesoDesbloqueado);
             System.out.println("Proceso " + procesoDesbloqueado.pid + " fue liberado del semáforo y regresó a la cola de listos.");
        }
    }
    
    public void forzarDeadlock() {
        if(colaListos.size() >= 2){
            PCB pA = colaListos.poll();
            PCB pB = colaListos.poll();
            gestorDeadlock.simularDeadlock(pA, pB);
        } else {
            System.out.println("Se necesitan al menos 2 procesos en la cola para simular un Deadlock.");
        }
    }
}

// --- CLASE PRINCIPAL (MAIN) ---
public class SimuladorSO {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PlanificadorRR planificador = new PlanificadorRR();
        boolean salir = false;

        while (!salir) {
            System.out.println("\n================================================");
            System.out.println(" Simulador del Nucleo de Sistema Operativo[cite: 3]");
            System.out.println("================================================");
            System.out.println("1. Crear nuevo proceso");
            System.out.println("2. Ejecutar un ciclo de CPU (Round Robin)");
            System.out.println("3. Forzar condicion de Deadlock[cite: 3]");
            System.out.println("4. Resolver Deadlock (Abortar proceso)[cite: 3]");
            System.out.println("5. Mostrar estado del sistema");
            System.out.println("6. Salir");
            System.out.print("Seleccione una opcion: ");
            
            String opcion = scanner.nextLine();

            switch (opcion) {
                case "1":
                    System.out.print("Ingrese tamaño en MB: ");
                    int tamano = Integer.parseInt(scanner.nextLine());
                    System.out.print("Ingrese rafaga de CPU: ");
                    int rafaga = Integer.parseInt(scanner.nextLine());
                    planificador.crearProceso(tamano, rafaga);
                    break;
                case "2":
                    planificador.ejecutarSiguiente();
                    break;
                case "3":
                    planificador.forzarDeadlock();
                    break;
                case "4":
                    if (planificador.gestorDeadlock.enDeadlock) {
                        System.out.println("Proceso A: " + planificador.gestorDeadlock.procesoA.pid);
                        System.out.println("Proceso B: " + planificador.gestorDeadlock.procesoB.pid);
                        System.out.print("¿Qué proceso desea abortar forzosamente (A/B)?: ");
                        String proc = scanner.nextLine().toUpperCase();
                        planificador.gestorDeadlock.abortarProceso(proc, planificador);
                    } else {
                        System.out.println("No hay deadlock activo.");
                    }
                    break;
                case "5":
                    System.out.println("\n--- Estado del Sistema ---");
                    System.out.println("Memoria RAM: " + planificador.memoria.memoriaDisponible + "MB Libres de " + planificador.memoria.capacidadTotal + "MB");
                    System.out.print("Cola de Listos: ");
                    for(PCB p : planificador.colaListos) {
                        System.out.print(p.pid + "(" + p.rafagaRestante + ") ");
                    }
                    System.out.println();
                    System.out.println("Estado Deadlock: " + (planificador.gestorDeadlock.enDeadlock ? "ACTIVO" : "Normal"));
                    break;
                case "6":
                    System.out.println("Saliendo del simulador...");
                    salir = true;
                    break;
                default:
                    System.out.println("Opción no valida.");
                    break;
            }
        }
        scanner.close();
    }
}
